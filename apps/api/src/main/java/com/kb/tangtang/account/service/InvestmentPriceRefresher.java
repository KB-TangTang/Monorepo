package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.stock.TossPriceClient;
import com.kb.tangtang.account.client.stock.TossPriceDto;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 보유종목 리스트를 받아 오래된 심볼만 토스 실시간 시세로 갱신한다(조회 시점 lazy refresh).
 *
 * ⚠ **심볼 단위 TTL 캐시다 — 사용자 단위가 아니다.** 삼성전자를 여러 사용자가 들고 있어도 토스
 *   호출은 TTL 안에서 한 번뿐이다. 캐시된 값은 이 메서드를 부르는 모든 holdings 리스트에 적용되고,
 *   적용될 때마다 각 행의 quantity·purchaseAmount 로 marketValue·profitLoss 를 다시 계산해
 *   DB({@link InvestmentHoldingMapper#updatePrice})에도 반영한다 — 그래야 지금 이 요청의 사용자가
 *   아닌 다른 사용자의 화면·자산 총액(netWorth, docs/API_SPEC.md 참고)도 다음에 읽을 때 이미
 *   최신 값을 보게 된다. DB 에 안 쓰고 응답 DTO 에만 끼워 넣으면 화면마다 가격이 어긋난다.
 *
 * ⚠ 토스 호출이 실패해도(레이트리밋·서버 오류·토큰 미준비) 캐시에 "방금 시도했다"로 남긴다 —
 *   그러지 않으면 토스가 장애 중일 때 요청마다 계속 재시도해서 오히려 부담이 늘어난다.
 *   실패한 심볼은 DB 저장값을 그대로 두고 다음 TTL 이후에 다시 시도한다.
 */
@Component
public class InvestmentPriceRefresher {

    private static final Logger log = LoggerFactory.getLogger(InvestmentPriceRefresher.class);
    private static final int SCALE = 4;

    private final TossPriceClient tossPriceClient;
    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final long ttlMillis;

    /** 테스트에서만 교체한다(패키지 전용 세터) — TossTokenHolder 와 같은 이유. */
    private volatile Clock clock = Clock.systemUTC();

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Object refreshLock = new Object();

    public InvestmentPriceRefresher(TossPriceClient tossPriceClient,
                                    InvestmentHoldingMapper investmentHoldingMapper,
                                    @Value("${toss.price.cache-ttl-ms}") long ttlMillis) {
        this.tossPriceClient = tossPriceClient;
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.ttlMillis = ttlMillis;
    }

    /**
     * holdings 를 그대로 최신화한다. 오래된 심볼만 토스에 물어 캐시·DB 를 갱신하고, 넘겨받은
     * 객체의 lastPrice·marketValue·profitLoss 필드도 같은 값으로 바꿔둔다 — 호출부가 다시
     * 조회하지 않아도 최신 값을 바로 쓸 수 있게 하기 위함.
     */
    public void refresh(List<InvestmentHolding> holdings) {
        if (holdings.isEmpty()) {
            return;
        }

        Set<String> staleSymbols = collectStaleSymbols(holdings);
        if (!staleSymbols.isEmpty()) {
            synchronized (refreshLock) {
                /* 락을 기다리는 동안 다른 스레드가 이미 갱신했을 수 있다 — 다시 걸러 중복 호출을 막는다. */
                staleSymbols = collectStaleSymbols(holdings);
                if (!staleSymbols.isEmpty()) {
                    fetchAndCache(staleSymbols);
                }
            }
        }

        for (InvestmentHolding holding : holdings) {
            CacheEntry entry = cache.get(holding.getSymbol());
            if (entry == null || entry.lastPrice == null) {
                continue;   // 캐시에 없거나(첫 시도 전) 마지막 시도가 실패했다 — DB 저장값을 그대로 둔다
            }
            applyPrice(holding, entry.lastPrice);
        }
    }

    private Set<String> collectStaleSymbols(List<InvestmentHolding> holdings) {
        Set<String> stale = new LinkedHashSet<>();
        for (InvestmentHolding holding : holdings) {
            if (isStale(cache.get(holding.getSymbol()))) {
                stale.add(holding.getSymbol());
            }
        }
        return stale;
    }

    private boolean isStale(CacheEntry entry) {
        return entry == null || Instant.now(clock).isAfter(entry.fetchedAt.plusMillis(ttlMillis));
    }

    private void fetchAndCache(Set<String> symbols) {
        Instant now = Instant.now(clock);
        Map<String, TossPriceDto> prices;
        try {
            prices = tossPriceClient.fetchPrices(symbols);
        } catch (BusinessException e) {
            log.warn("토스 시세 갱신 실패, 저장된 값을 그대로 쓴다 symbols={}", symbols, e);
            prices = Map.of();
        }
        for (String symbol : symbols) {
            TossPriceDto price = prices.get(symbol);
            BigDecimal lastPrice = price == null ? null : price.getLastPrice();
            cache.put(symbol, new CacheEntry(lastPrice, now));
        }
    }

    /** marketValue = lastPrice * quantity, profitLossAmount = marketValue - purchaseAmount. */
    private void applyPrice(InvestmentHolding holding, BigDecimal lastPrice) {
        BigDecimal marketValue = lastPrice.multiply(holding.getQuantity())
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal profitLossAmount = marketValue.subtract(holding.getPurchaseAmount())
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal profitLossRate = holding.getPurchaseAmount().signum() == 0
                ? BigDecimal.ZERO.setScale(SCALE)
                : profitLossAmount.divide(holding.getPurchaseAmount(), SCALE, RoundingMode.HALF_UP);

        holding.setLastPrice(lastPrice);
        holding.setMarketValue(marketValue);
        holding.setProfitLossAmount(profitLossAmount);
        holding.setProfitLossRate(profitLossRate);

        investmentHoldingMapper.updatePrice(holding);
    }

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    private static final class CacheEntry {
        private final BigDecimal lastPrice;
        private final Instant fetchedAt;

        private CacheEntry(BigDecimal lastPrice, Instant fetchedAt) {
            this.lastPrice = lastPrice;
            this.fetchedAt = fetchedAt;
        }
    }
}

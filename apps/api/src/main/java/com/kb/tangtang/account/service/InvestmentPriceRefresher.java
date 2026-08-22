package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.stock.TossPriceClient;
import com.kb.tangtang.account.client.stock.TossPriceDto;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
 *
 * ⚠ DB 쓰기 구간만 트랜잭션으로 묶는다(QA 지적사항) — 토스 호출(fetchAndCache)이 끝난 **뒤에만**
 *   트랜잭션을 열어 holdings 를 한꺼번에 갱신한다. 그러지 않으면(전체를 트랜잭션으로 감싸면) 이
 *   서비스를 부르는 쪽이 @Transactional 을 다시 둘 이유가 생기고, 그러면 블로킹 HTTP 호출 동안
 *   DB 커넥션을 붙잡는 원래 문제로 되돌아간다. 트랜잭션 개시·커밋은 FinancialSyncServiceImpl 과
 *   같은 TransactionTemplate 패턴을 쓴다 — 자기호출(같은 객체의 메서드를 직접 부름)이라 @Transactional
 *   애너테이션은 프록시를 못 타서 효과가 없다.
 *
 * ⚠ apps/api/AGENTS.md 는 "트랜잭션 경계는 Service 에 둔다"고 못박지만, 이 클래스는 @Component 다.
 *   일부러 어긴 게 아니라 — 경계를 호출부(AssetInvestmentDetailService·AssetCompositionCalculator,
 *   둘 다 Service)에 두면 바로 위 문단의 "블로킹 호출 동안 커넥션을 붙잡는" 문제가 되돌아오기
 *   때문에, "이 클래스의 DB 쓰기 구간만" 이라는 더 좁은 경계가 필요해서 여기로 내렸다. 이 클래스
 *   자체가 하나의 원자적 단위(오래된 심볼 갱신)를 표현하는 도메인 서비스에 가깝다고 보면 된다.
 */
@Component
public class InvestmentPriceRefresher {

    private static final Logger log = LoggerFactory.getLogger(InvestmentPriceRefresher.class);
    private static final int SCALE = 4;

    private final TossPriceClient tossPriceClient;
    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final TransactionTemplate transactionTemplate;
    private final long ttlMillis;

    /** 테스트에서만 교체한다(패키지 전용 세터) — TossTokenHolder 와 같은 이유. */
    private volatile Clock clock = Clock.systemUTC();

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Object refreshLock = new Object();

    public InvestmentPriceRefresher(TossPriceClient tossPriceClient,
                                    InvestmentHoldingMapper investmentHoldingMapper,
                                    TransactionTemplate transactionTemplate,
                                    @Value("${toss.price.cache-ttl-ms}") long ttlMillis) {
        this.tossPriceClient = tossPriceClient;
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.transactionTemplate = transactionTemplate;
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

        /*
         * 캐시에 유효한 가격이 있는 holding만 가격과 함께 추린다 — 하나도 없으면 트랜잭션을 열
         * 이유가 없다. ⚠ 여기서 가격을 같이 붙잡아 둔다(QA 지적사항) — 트랜잭션 콜백 안에서
         * cache.get() 을 다시 읽으면, 이 시점과 콜백 실행 시점 사이(예: 커넥션 풀이 바빠 대기하는
         * 동안) 다른 스레드가 같은 심볼을 TTL 만료로 다시 갱신하면서 캐시를 "실패"(lastPrice=null)로
         * 덮어쓸 수 있다. 그러면 이미 유효했던 가격이 null 로 바뀌어 applyPrice 가 NPE 로 죽고,
         * 그 심볼과 무관한 나머지 holding 들까지 전부 롤백된다.
         */
        List<PricedHolding> toApply = new ArrayList<>();
        for (InvestmentHolding holding : holdings) {
            CacheEntry entry = cache.get(holding.getSymbol());
            if (entry != null && entry.lastPrice != null) {
                toApply.add(new PricedHolding(holding, entry.lastPrice));
            }
        }
        if (toApply.isEmpty()) {
            return;   // 캐시에 없거나(첫 시도 전) 전부 실패했다 — DB 저장값을 그대로 둔다
        }

        /*
         * 여기서부터는 순수 DB 쓰기뿐이라(토스 호출은 이미 끝났다) 트랜잭션으로 묶어도 커넥션을
         * 오래 붙잡지 않는다 — 한 종목 쓰다가 실패해도 이 holdings 묶음 전체가 롤백돼, 일부만
         * 새 가격이고 나머지는 옛 가격인 반쪽짜리 상태가 남지 않는다(QA 지적사항).
         */
        transactionTemplate.executeWithoutResult(status -> {
            for (PricedHolding priced : toApply) {
                applyPrice(priced.holding(), priced.lastPrice());
            }
        });
    }

    /** toApply 를 만들 때 캐시에서 읽은 가격을 그대로 붙잡아 두는 용도 — 트랜잭션 콜백 안에서 캐시를 다시 읽지 않기 위함. */
    private record PricedHolding(InvestmentHolding holding, BigDecimal lastPrice) {
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
        } catch (RuntimeException e) {
            /*
             * BusinessException 뿐 아니라 RuntimeException 전체를 잡는다(QA 지적사항). 토스가
             * 문서에 없는 응답 모양을 주면 TossPriceClient 안에서 ClassCastException·NPE 같은
             * 미확인 예외가 날 수 있는데, 그것까지 여기서 막아야 이 요청 전체가 500 으로 죽지 않고
             * "이번엔 못 받았다 → 저장된 값 유지"라는 원래 설계대로 흘러간다.
             */
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
        /*
         * 캐시가 fresh 라도(=이번 호출에서 토스를 다시 안 불렀어도) 여기까지 온 모든 holding 에
         * 대해 매번 UPDATE 를 쐈었다(QA 지적사항) — 값이 그대로면 쓸 것도 없다. compareTo 로
         * 비교하는 이유는 BigDecimal.equals() 가 스케일까지 따져서(예: "72000" vs "72000.0000")
         * 값이 같아도 다르다고 판정하기 때문이다.
         */
        if (holding.getLastPrice() != null && holding.getLastPrice().compareTo(lastPrice) == 0) {
            return;
        }

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

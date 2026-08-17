package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.stock.TossPriceClient;
import com.kb.tangtang.account.client.stock.TossPriceDto;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestmentPriceRefresherTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    private TossPriceClient tossPriceClient;
    private InvestmentHoldingMapper investmentHoldingMapper;
    private InvestmentPriceRefresher refresher;

    private void setUp(long ttlMillis) {
        tossPriceClient = mock(TossPriceClient.class);
        investmentHoldingMapper = mock(InvestmentHoldingMapper.class);
        refresher = new InvestmentPriceRefresher(tossPriceClient, investmentHoldingMapper, ttlMillis);
        refresher.setClockForTest(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private InvestmentHolding holding(long id, String symbol, String quantity, String purchaseAmount) {
        return InvestmentHolding.builder()
                .id(id)
                .symbol(symbol)
                .quantity(new BigDecimal(quantity))
                .purchaseAmount(new BigDecimal(purchaseAmount))
                .build();
    }

    @Test
    @DisplayName("가격을 받으면 quantity·purchaseAmount로 marketValue·profitLoss를 계산해 각 행에 반영하고 DB에 쓴다")
    void appliesFetchedPriceAndUpdatesDb() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        when(tossPriceClient.fetchPrices(Set.of("005930"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build()));

        refresher.refresh(List.of(holding));

        assertEquals(0, new BigDecimal("72000").compareTo(holding.getLastPrice()));
        assertEquals(0, new BigDecimal("720000").compareTo(holding.getMarketValue()));
        assertEquals(0, new BigDecimal("20000").compareTo(holding.getProfitLossAmount()));
        /* 20000 / 700000 = 0.028571... → HALF_UP 4자리 = 0.0286 */
        assertEquals(0, new BigDecimal("0.0286").compareTo(holding.getProfitLossRate()));
        verify(investmentHoldingMapper).updatePrice(holding);
    }

    @Test
    @DisplayName("원금이 0이면 수익률은 0으로 둔다(0으로 나누지 않는다)")
    void zeroPurchaseAmountYieldsZeroRate() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "0");
        when(tossPriceClient.fetchPrices(Set.of("005930"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build()));

        refresher.refresh(List.of(holding));

        assertEquals(0, BigDecimal.ZERO.compareTo(holding.getProfitLossRate()));
    }

    @Test
    @DisplayName("TTL 안에서는 같은 심볼을 다시 조회해도 토스를 또 부르지 않는다")
    void reusesCachedPriceWithinTtl() {
        setUp(20_000);
        InvestmentHolding holdingA = holding(1L, "005930", "10", "700000");
        InvestmentHolding holdingB = holding(2L, "005930", "5", "300000");
        when(tossPriceClient.fetchPrices(Set.of("005930"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build()));

        refresher.refresh(List.of(holdingA));
        refresher.refresh(List.of(holdingB));

        verify(tossPriceClient, times(1)).fetchPrices(Set.of("005930"));
        assertEquals(0, new BigDecimal("72000").compareTo(holdingB.getLastPrice()));
        verify(investmentHoldingMapper).updatePrice(holdingA);
        verify(investmentHoldingMapper).updatePrice(holdingB);
    }

    @Test
    @DisplayName("TTL이 지나면 다시 토스를 부른다")
    void refetchesAfterTtlExpires() {
        setUp(1_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        when(tossPriceClient.fetchPrices(Set.of("005930"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build()));

        refresher.refresh(List.of(holding));
        refresher.setClockForTest(Clock.fixed(NOW.plusMillis(1_001), ZoneOffset.UTC));
        refresher.refresh(List.of(holding));

        verify(tossPriceClient, times(2)).fetchPrices(Set.of("005930"));
    }

    @Test
    @DisplayName("토스 호출이 실패해도 예외를 던지지 않고 저장된 값을 그대로 둔다")
    void keepsStoredValueWhenFetchFails() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        holding.setLastPrice(new BigDecimal("70000"));
        when(tossPriceClient.fetchPrices(Set.of("005930")))
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "실패"));

        refresher.refresh(List.of(holding));

        assertEquals(0, new BigDecimal("70000").compareTo(holding.getLastPrice()));
        verify(investmentHoldingMapper, never()).updatePrice(any());
    }

    @Test
    @DisplayName("실패도 TTL 동안은 캐시돼 재시도 폭주를 막는다")
    void failureIsCachedToAvoidRetryStorm() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        when(tossPriceClient.fetchPrices(Set.of("005930")))
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "실패"));

        refresher.refresh(List.of(holding));
        refresher.refresh(List.of(holding));

        verify(tossPriceClient, times(1)).fetchPrices(Set.of("005930"));
    }

    @Test
    @DisplayName("빈 리스트는 아무 일도 하지 않는다")
    void doesNothingForEmptyList() {
        setUp(20_000);
        refresher.refresh(List.of());
        verify(tossPriceClient, never()).fetchPrices(any());
    }
}

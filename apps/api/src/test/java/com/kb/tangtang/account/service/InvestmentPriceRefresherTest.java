package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.stock.TossPriceClient;
import com.kb.tangtang.account.client.stock.TossPriceDto;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    /** 트랜잭션 개시·커밋·롤백 시점을 기록한다 — FinancialSyncServiceImplTest 와 같은 패턴. */
    private List<String> timeline;

    private PlatformTransactionManager recordingTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                timeline.add("BEGIN");
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                timeline.add("COMMIT");
            }

            @Override
            public void rollback(TransactionStatus status) {
                timeline.add("ROLLBACK");
            }
        };
    }

    private void setUp(long ttlMillis) {
        tossPriceClient = mock(TossPriceClient.class);
        investmentHoldingMapper = mock(InvestmentHoldingMapper.class);
        timeline = new ArrayList<>();
        refresher = new InvestmentPriceRefresher(tossPriceClient, investmentHoldingMapper,
                new TransactionTemplate(recordingTransactionManager()), ttlMillis);
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

    @Test
    @DisplayName("가격이 이미 최신값과 같으면 DB UPDATE를 건너뛴다")
    void skipsUpdateWhenPriceUnchanged() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        holding.setLastPrice(new BigDecimal("72000"));   // 이미 이번에 받을 가격과 같다
        when(tossPriceClient.fetchPrices(Set.of("005930"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build()));

        refresher.refresh(List.of(holding));

        verify(investmentHoldingMapper, never()).updatePrice(any());
    }

    @Test
    @DisplayName("BusinessException이 아닌 런타임 예외도 삼키고 저장된 값을 그대로 둔다")
    void keepsStoredValueOnUnexpectedRuntimeException() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        holding.setLastPrice(new BigDecimal("70000"));
        /* TossPriceClient가 토스의 예상 밖 응답을 파싱하다 던질 수 있는 미확인 예외를 흉내낸다
           (예: ClassCastException) — BusinessException이 아니어도 요청 전체를 500으로 만들면 안 된다. */
        when(tossPriceClient.fetchPrices(Set.of("005930")))
                .thenThrow(new NullPointerException("토스 응답을 예상과 다른 형태로 파싱했다"));

        refresher.refresh(List.of(holding));

        assertEquals(0, new BigDecimal("70000").compareTo(holding.getLastPrice()));
        verify(investmentHoldingMapper, never()).updatePrice(any());
    }

    @Test
    @DisplayName("DB 쓰기 구간은 한 트랜잭션으로 묶인다 — 한 종목에서 실패하면 전체가 롤백된다(QA 지적사항)")
    void rollsBackWholeBatchOnPartialDbFailure() {
        setUp(20_000);
        InvestmentHolding holdingA = holding(1L, "005930", "10", "700000");
        InvestmentHolding holdingB = holding(2L, "000660", "5", "300000");
        when(tossPriceClient.fetchPrices(Set.of("005930", "000660"))).thenReturn(Map.of(
                "005930", TossPriceDto.builder().symbol("005930").lastPrice(new BigDecimal("72000")).build(),
                "000660", TossPriceDto.builder().symbol("000660").lastPrice(new BigDecimal("210000")).build()));
        doThrow(new RuntimeException("DB 오류")).when(investmentHoldingMapper).updatePrice(holdingB);

        assertThrows(RuntimeException.class, () -> refresher.refresh(List.of(holdingA, holdingB)));

        /* holdingA 는 순서상 먼저 쓰기를 시도했지만, 같은 트랜잭션 안에서 holdingB 가 실패하면서
           통째로 롤백된다 — COMMIT 이 아니라 ROLLBACK 이 기록돼야 한다. */
        verify(investmentHoldingMapper).updatePrice(holdingA);
        assertEquals(List.of("BEGIN", "ROLLBACK"), timeline);
    }

    @Test
    @DisplayName("토스 호출은 트랜잭션 밖에서 일어난다 — DB 쓰기가 없으면 트랜잭션을 열지 않는다")
    void doesNotOpenTransactionWhenNothingToWrite() {
        setUp(20_000);
        InvestmentHolding holding = holding(1L, "005930", "10", "700000");
        when(tossPriceClient.fetchPrices(Set.of("005930")))
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "실패"));

        refresher.refresh(List.of(holding));

        assertEquals(0, timeline.size());
    }
}

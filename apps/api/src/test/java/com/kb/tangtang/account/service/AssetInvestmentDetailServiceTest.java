package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.dto.AssetInvestmentDetailDto;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetInvestmentDetailServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private InvestmentHoldingMapper investmentHoldingMapper;

    private AssetInvestmentDetailService service() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(
                LocalDate.of(2026, 8, 15).atTime(9, 41).atZone(zoneId).toInstant(), zoneId);
        return new AssetInvestmentDetailService(investmentHoldingMapper, clock);
    }

    private InvestmentHolding holding(String symbol, String name, long marketValue, long purchaseAmount,
                                       long profitLossAmount, String profitLossRate) {
        return InvestmentHolding.builder()
                .id(1L)
                .userId(USER_ID)
                .accountId(10L)
                .symbol(symbol)
                .name(name)
                .marketCountry("KR")
                .currency("KRW")
                .quantity(new BigDecimal("20"))
                .averagePurchasePrice(new BigDecimal("75000"))
                .lastPrice(new BigDecimal("80000"))
                .purchaseAmount(BigDecimal.valueOf(purchaseAmount))
                .marketValue(BigDecimal.valueOf(marketValue))
                .profitLossAmount(BigDecimal.valueOf(profitLossAmount))
                .profitLossRate(new BigDecimal(profitLossRate))
                .build();
    }

    @Test
    @DisplayName("보유 종목의 평가금액·원금을 합산하고 asOf 는 조회 시각을 그대로 쓴다")
    void aggregatesHoldings() {
        when(investmentHoldingMapper.findByUser(USER_ID)).thenReturn(List.of(
                holding("005930", "삼성전자", 1_600_000, 1_500_000, 100_000, "0.0667"),
                holding("035720", "카카오", 500_000, 480_000, 20_000, "0.0417")
        ));

        AssetInvestmentDetailDto result = service().getInvestments(USER_ID);

        assertEquals(new BigDecimal("2100000"), result.getTotalValuation());
        assertEquals(new BigDecimal("1980000"), result.getTotalCost());
        assertEquals(LocalDateTime.of(2026, 8, 15, 9, 41), result.getAsOf());
        assertEquals(2, result.getHoldings().size());
        assertEquals("삼성전자", result.getHoldings().get(0).getName());
        assertEquals(new BigDecimal("100000"), result.getHoldings().get(0).getProfitLossAmount());
    }

    @Test
    @DisplayName("보유 종목이 없으면 0 원과 빈 목록을 돌려준다")
    void returnsZeroWhenNoHoldings() {
        when(investmentHoldingMapper.findByUser(USER_ID)).thenReturn(List.of());

        AssetInvestmentDetailDto result = service().getInvestments(USER_ID);

        assertEquals(BigDecimal.ZERO, result.getTotalValuation());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertEquals(0, result.getHoldings().size());
    }
}

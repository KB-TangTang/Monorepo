package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetTrendDto;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetTrendServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Mock
    private AssetCompositionCalculator compositionCalculator;

    @Mock
    private AssetNetWorthTrendService trendService;

    private AssetTrendService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(TODAY.atTime(10, 30).atZone(zoneId).toInstant(), zoneId);
        service = new AssetTrendService(compositionCalculator, trendService, clock);
    }

    @Test
    @DisplayName("compositionCalculator 의 라이브 값을 trendService 에 넘겨 추이를 조립한다")
    void returnsTrend() {
        when(compositionCalculator.compute(USER_ID)).thenReturn(AssetLiveComposition.builder()
                .netWorth(new BigDecimal("9445500"))
                .totalDebt(new BigDecimal("1500000"))
                .composition(List.of())
                .assetGroups(List.of())
                .build());
        List<AssetTrendItemDto> trend = List.of(AssetTrendItemDto.builder()
                .yearMonth("2026-08").netWorth(new BigDecimal("9445500")).totalDebt(new BigDecimal("1500000")).build());
        when(trendService.buildTrend(USER_ID, YearMonth.of(2026, 8),
                new BigDecimal("9445500"), new BigDecimal("1500000"))).thenReturn(trend);

        AssetTrendDto result = service.getTrend(USER_ID, null);

        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 30), result.getAsOf());
        assertSame(trend, result.getTrend());
    }

    @Test
    @DisplayName("baseDate 형식이 잘못되면 INVALID_REQUEST 예외를 던진다")
    void throwsOnInvalidBaseDate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getTrend(USER_ID, "2026/08/15"));

        assertEquals("INVALID_REQUEST", ex.getCode());
    }
}

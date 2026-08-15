package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.dto.AssetSummaryDto;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 순자산·구성 계산은 {@link AssetCompositionCalculator}, 6개월 추이 계산은 {@link AssetNetWorthTrendService}
 * 로 옮겨졌다 — 이 테스트는 두 협력자의 결과를 AssetSummaryService 가 올바르게 조립하는지만 본다.
 * 각 계산 로직 자체의 세부 케이스(PAYMONEY 정규화, 스냅샷 null 채움 등)는 그 클래스들의 테스트가 맡는다.
 */
@ExtendWith(MockitoExtension.class)
class AssetSummaryServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Mock
    private AssetCompositionCalculator compositionCalculator;

    @Mock
    private AssetNetWorthTrendService trendService;

    private AssetSummaryService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(TODAY);
    }

    private AssetSummaryService serviceAt(LocalDate currentDate) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(currentDate.atTime(10, 30).atZone(zoneId).toInstant(), zoneId);
        return new AssetSummaryService(compositionCalculator, trendService, clock);
    }

    private AssetLiveComposition liveComposition(BigDecimal netWorth, BigDecimal totalDebt,
                                                  List<AssetCompositionItemDto> composition,
                                                  List<AssetGroupItemDto> assetGroups) {
        return AssetLiveComposition.builder()
                .netWorth(netWorth)
                .totalDebt(totalDebt)
                .composition(composition)
                .assetGroups(assetGroups)
                .build();
    }

    /** 마지막 두 항목이 각각 전월·baseMonth 가 되도록 6개짜리 trend 를 만든다(AssetNetWorthTrendService 규약). */
    private List<AssetTrendItemDto> trendEndingWith(BigDecimal previousMonthNetWorth, BigDecimal previousMonthTotalDebt,
                                                      BigDecimal currentNetWorth, BigDecimal currentTotalDebt) {
        List<AssetTrendItemDto> trend = new ArrayList<>();
        for (int i = 3; i <= 6; i++) {
            trend.add(AssetTrendItemDto.builder().yearMonth("2026-0" + i).netWorth(null).totalDebt(null).build());
        }
        trend.set(4, AssetTrendItemDto.builder().yearMonth("2026-07")
                .netWorth(previousMonthNetWorth).totalDebt(previousMonthTotalDebt).build());
        trend.set(5, AssetTrendItemDto.builder().yearMonth("2026-08")
                .netWorth(currentNetWorth).totalDebt(currentTotalDebt).build());
        return trend;
    }

    @Test
    @DisplayName("compositionCalculator·trendService 결과로 요약을 조립한다")
    void returnsAssetSummary() {
        List<AssetCompositionItemDto> composition = List.of(AssetCompositionItemDto.builder()
                .type("DEMAND_DEPOSIT").label("입출금").amount(new BigDecimal("2066800")).build());
        List<AssetGroupItemDto> assetGroups = List.of(AssetGroupItemDto.builder()
                .type("DEMAND_DEPOSIT").label("입출금 계좌").count(2).amount(new BigDecimal("2066800")).build());
        when(compositionCalculator.compute(USER_ID)).thenReturn(
                liveComposition(new BigDecimal("9445500"), new BigDecimal("1500000"), composition, assetGroups));
        List<AssetTrendItemDto> trend = trendEndingWith(null, null, new BigDecimal("9445500"), new BigDecimal("1500000"));
        when(trendService.buildTrend(USER_ID, YearMonth.of(2026, 8),
                new BigDecimal("9445500"), new BigDecimal("1500000"))).thenReturn(trend);

        AssetSummaryDto result = service.getSummary(USER_ID, null);

        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 30), result.getAsOf());
        assertEquals(new BigDecimal("9445500"), result.getNetWorth());
        assertNull(result.getPreviousMonthNetWorth());
        assertNull(result.getChangeAmount());
        assertNull(result.getChangeRate());
        assertSame(composition, result.getComposition());
        assertSame(assetGroups, result.getAssetGroups());
        assertSame(trend, result.getTrend());
    }

    @Test
    @DisplayName("전월 스냅샷이 있으면 증감액·증감률을 계산한다")
    void calculatesChangeWhenPreviousSnapshotExists() {
        when(compositionCalculator.compute(USER_ID)).thenReturn(
                liveComposition(new BigDecimal("9445500"), BigDecimal.ZERO, List.of(), List.of()));
        List<AssetTrendItemDto> trend = trendEndingWith(
                new BigDecimal("9125500"), new BigDecimal("1600000"), new BigDecimal("9445500"), BigDecimal.ZERO);
        when(trendService.buildTrend(eq(USER_ID), eq(YearMonth.of(2026, 8)), any(), any())).thenReturn(trend);

        AssetSummaryDto result = service.getSummary(USER_ID, "2026-08-15");

        assertEquals(new BigDecimal("9125500"), result.getPreviousMonthNetWorth());
        assertEquals(new BigDecimal("320000"), result.getChangeAmount());
        assertEquals(new BigDecimal("3.51"), result.getChangeRate());
    }

    @Test
    @DisplayName("baseDate 형식이 잘못되면 INVALID_REQUEST 예외를 던진다")
    void throwsOnInvalidBaseDate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026/08/15"));

        assertEquals("INVALID_REQUEST", ex.getCode());
    }
}

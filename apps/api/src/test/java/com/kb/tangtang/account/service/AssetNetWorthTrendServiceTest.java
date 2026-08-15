package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetSnapshotRow;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * GET /api/assets/summary·GET /api/assets/trend 가 공유하는 6개월 추이 계산 로직 자체를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AssetNetWorthTrendServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private AssetSummaryMapper assetSummaryMapper;

    private AssetNetWorthTrendService service;

    @BeforeEach
    void setUp() {
        service = new AssetNetWorthTrendService(assetSummaryMapper);
    }

    @Test
    @DisplayName("스냅샷이 없는 과거월은 null, baseMonth 는 호출자가 넘긴 라이브 값으로 채운 6개월을 반환한다")
    void buildsTrendWithLiveCurrentMonth() {
        when(assetSummaryMapper.findNetWorthSnapshots(eq(USER_ID), anyList())).thenReturn(List.of());

        List<AssetTrendItemDto> trend = service.buildTrend(
                USER_ID, YearMonth.of(2026, 8), new BigDecimal("9445500"), new BigDecimal("1500000"));

        assertEquals(6, trend.size());
        assertEquals("2026-03", trend.get(0).getYearMonth());
        assertNull(trend.get(0).getNetWorth());
        assertNull(trend.get(0).getTotalDebt());

        AssetTrendItemDto last = trend.get(trend.size() - 1);
        assertEquals("2026-08", last.getYearMonth());
        assertEquals(new BigDecimal("9445500"), last.getNetWorth());
        assertEquals(new BigDecimal("1500000"), last.getTotalDebt());
    }

    @Test
    @DisplayName("과거월 스냅샷이 있으면 그 값을 그대로 채운다")
    void fillsPriorMonthFromSnapshot() {
        when(assetSummaryMapper.findNetWorthSnapshots(eq(USER_ID), anyList())).thenReturn(List.of(
                new AssetSnapshotRow("2026-07", new BigDecimal("9125500"), new BigDecimal("1600000"))
        ));

        List<AssetTrendItemDto> trend = service.buildTrend(
                USER_ID, YearMonth.of(2026, 8), new BigDecimal("9445500"), new BigDecimal("1500000"));

        AssetTrendItemDto julyPoint = trend.stream()
                .filter(item -> item.getYearMonth().equals("2026-07")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("9125500"), julyPoint.getNetWorth());
        assertEquals(new BigDecimal("1600000"), julyPoint.getTotalDebt());
    }
}

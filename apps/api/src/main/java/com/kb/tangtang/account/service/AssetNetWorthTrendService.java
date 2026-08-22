package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetSnapshotRow;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 최근 6개월 순자산·부채 추이를 만든다. {@link AssetSummaryService}(요약 카드의 trend 필드)와
 * {@link AssetTrendService}(전용 추이 API) 가 공통으로 쓴다.
 *
 * 과거월은 tbl_asset_snapshot(현재는 매월 초 배치 {@code MonthlyReportBatchScheduler} 또는
 * {@code POST /api/reports/monthly/ai-analysis} 호출 시 채워짐)을 조회해 net_worth 가 있으면 쓰고,
 * 없으면 해당 월은 null 로 반환한다. baseMonth(요청 기준월)만은 스냅샷 유무와 무관하게 호출자가 넘긴
 * 라이브 값을 그대로 채운다 — asOf 카드와 추이 그래프의 마지막 점이 항상 같은 값이도록 하기 위함이다.
 */
@Service
public class AssetNetWorthTrendService {

    private static final int TREND_MONTH_COUNT = 6;

    private final AssetSummaryMapper assetSummaryMapper;

    @Autowired
    public AssetNetWorthTrendService(AssetSummaryMapper assetSummaryMapper) {
        this.assetSummaryMapper = assetSummaryMapper;
    }

    /**
     * @param currentNetWorth baseMonth 의 라이브 순자산 (호출자가 이미 계산해 둔 값)
     * @param currentTotalDebt baseMonth 의 라이브 대출잔액
     */
    public List<AssetTrendItemDto> buildTrend(long userId, YearMonth baseMonth,
                                               BigDecimal currentNetWorth, BigDecimal currentTotalDebt) {
        List<YearMonth> priorMonths = new ArrayList<>();
        for (int i = TREND_MONTH_COUNT - 1; i >= 1; i--) {
            priorMonths.add(baseMonth.minusMonths(i));
        }
        List<String> priorMonthKeys = new ArrayList<>();
        for (YearMonth month : priorMonths) {
            priorMonthKeys.add(month.toString());
        }
        Map<String, AssetSnapshotRow> snapshotByMonth = new LinkedHashMap<>();
        for (AssetSnapshotRow row : assetSummaryMapper.findNetWorthSnapshots(userId, priorMonthKeys)) {
            snapshotByMonth.put(row.getYearMonth(), row);
        }

        List<AssetTrendItemDto> trend = new ArrayList<>();
        for (YearMonth month : priorMonths) {
            AssetSnapshotRow row = snapshotByMonth.get(month.toString());
            trend.add(AssetTrendItemDto.builder()
                    .yearMonth(month.toString())
                    .netWorth(row == null ? null : row.getNetWorth())
                    .totalDebt(row == null ? null : row.getTotalDebt())
                    .build());
        }
        trend.add(AssetTrendItemDto.builder()
                .yearMonth(baseMonth.toString())
                .netWorth(currentNetWorth)
                .totalDebt(currentTotalDebt)
                .build());

        return trend;
    }
}

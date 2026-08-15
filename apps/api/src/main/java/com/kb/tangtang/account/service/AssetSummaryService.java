package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * 자산 현황(GET /api/assets/summary) 집계.
 *
 * 순자산·구성 계산은 {@link AssetCompositionCalculator}, 6개월 추이 계산은
 * {@link AssetNetWorthTrendService} 에 위임한다 — 둘 다 GET /api/assets/trend(AssetTrendService)와
 * 공유하는 로직이라 이 클래스에 다시 두지 않는다.
 *
 * 전월 비교(previousMonthNetWorth 등)는 trend 의 마지막에서 두 번째 항목(= baseMonth 의 전월)을
 * 그대로 쓴다 — trend 는 항상 baseMonth 를 포함한 6개월이라 별도 조회가 필요 없다.
 */
@Service
public class AssetSummaryService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AssetCompositionCalculator compositionCalculator;
    private final AssetNetWorthTrendService trendService;
    private final Clock clock;

    @Autowired
    public AssetSummaryService(AssetCompositionCalculator compositionCalculator,
                                AssetNetWorthTrendService trendService) {
        this(compositionCalculator, trendService, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AssetSummaryService(AssetCompositionCalculator compositionCalculator,
                         AssetNetWorthTrendService trendService, Clock clock) {
        this.compositionCalculator = compositionCalculator;
        this.trendService = trendService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssetSummaryDto getSummary(long userId, String rawBaseDate) {
        LocalDateTime asOf = LocalDateTime.now(clock);
        YearMonth baseMonth = YearMonth.from(AssetBaseDateParser.parse(rawBaseDate, asOf.toLocalDate()));

        AssetLiveComposition live = compositionCalculator.compute(userId);
        List<AssetTrendItemDto> trend = trendService.buildTrend(
                userId, baseMonth, live.getNetWorth(), live.getTotalDebt());

        AssetTrendItemDto previousMonthPoint = trend.get(trend.size() - 2);
        BigDecimal previousMonthNetWorth = previousMonthPoint.getNetWorth();
        BigDecimal changeAmount = previousMonthNetWorth == null
                ? null : live.getNetWorth().subtract(previousMonthNetWorth);
        BigDecimal changeRate = previousMonthNetWorth == null
                ? null : calculateChangeRate(live.getNetWorth(), previousMonthNetWorth);

        return AssetSummaryDto.builder()
                .asOf(asOf)
                .netWorth(live.getNetWorth())
                .previousMonthNetWorth(previousMonthNetWorth)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .trend(trend)
                .composition(live.getComposition())
                .assetGroups(live.getAssetGroups())
                .build();
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return current.signum() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : null;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}

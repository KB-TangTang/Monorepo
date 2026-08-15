package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetTrendDto;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * 자산 홈의 "추이 보기" 상세 화면(GET /api/assets/trend) 전용 조회.
 *
 * 6개월 추이 계산 자체는 {@link AssetSummaryService} 와 같은 {@link AssetCompositionCalculator}·
 * {@link AssetNetWorthTrendService} 를 그대로 재사용한다 — 로직을 복붙하지 않는다.
 * 자산 홈 스파크라인은 이미 GET /api/assets/summary 응답의 trend 를 그대로 쓰고 있어(같은 화면에서
 * summary 를 어차피 호출함) 이 엔드포인트로 옮기지 않았다.
 */
@Service
public class AssetTrendService {

    private final AssetCompositionCalculator compositionCalculator;
    private final AssetNetWorthTrendService trendService;
    private final Clock clock;

    @Autowired
    public AssetTrendService(AssetCompositionCalculator compositionCalculator,
                              AssetNetWorthTrendService trendService) {
        this(compositionCalculator, trendService, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AssetTrendService(AssetCompositionCalculator compositionCalculator,
                       AssetNetWorthTrendService trendService, Clock clock) {
        this.compositionCalculator = compositionCalculator;
        this.trendService = trendService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssetTrendDto getTrend(long userId, String rawBaseDate) {
        LocalDateTime asOf = LocalDateTime.now(clock);
        YearMonth baseMonth = YearMonth.from(AssetBaseDateParser.parse(rawBaseDate, asOf.toLocalDate()));

        AssetLiveComposition live = compositionCalculator.compute(userId);
        List<AssetTrendItemDto> trend = trendService.buildTrend(
                userId, baseMonth, live.getNetWorth(), live.getTotalDebt());

        return AssetTrendDto.builder()
                .asOf(asOf)
                .trend(trend)
                .build();
    }
}

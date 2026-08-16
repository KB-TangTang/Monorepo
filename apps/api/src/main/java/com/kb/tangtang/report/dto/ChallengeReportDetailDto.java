package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ChallengeReportDetailDto {

    private String period;
    private boolean hasChallengeHistory;
    private boolean isFirstServiceMonth;
    private boolean hasPreviousComparison;
    private BigDecimal missionSuccessRate;
    private BigDecimal monthOverMonthPercentagePoint;
    private int successfulDays;
    private int challengeDays;
    private int bestStreakDays;
    private String bestWeekday;
    private int earnedPoints;
    private BigDecimal savedAmount;
    private BigDecimal overspentAmount;
    private BigDecimal netSavings;
    private BigDecimal annualizedNetSavings;
    private List<ChallengeCategoryEffectDto> categoryEffects;
    private List<ChallengeWeeklyResultDto> weeklyResults;
    private List<ChallengeDifficultyResultDto> difficulties;
}

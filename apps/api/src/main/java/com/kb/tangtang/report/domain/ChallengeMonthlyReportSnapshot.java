package com.kb.tangtang.report.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChallengeMonthlyReportSnapshot {

    private long userId;
    private String yearMonth;
    private int totalDays;
    private int successDays;
    private BigDecimal savedAmount;
    private BigDecimal overspentAmount;
    private String categoryEffectsJson;
    private int monthlyLongestStreak;
    private String bestWeekday;
    private int earnedScore;
    private String weeklyResultsJson;
    private String difficultyResultsJson;
    private String groupRecordJson;
    private String calculationVersion;
    private LocalDateTime finalizedAt;
}

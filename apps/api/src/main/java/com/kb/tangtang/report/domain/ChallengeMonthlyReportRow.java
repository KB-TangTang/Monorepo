package com.kb.tangtang.report.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeMonthlyReportRow {

    private String yearMonth;
    private int totalDays;
    private int successDays;
    private BigDecimal savedAmount;
    private BigDecimal overspentAmount;
    private BigDecimal netAmount;
    private String categoryEffectsJson;
    private int monthlyLongestStreak;
    private String bestWeekday;
    private int earnedScore;
    private String weeklyResultsJson;
    private String difficultyResultsJson;
}

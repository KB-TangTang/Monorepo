package com.kb.tangtang.report.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeMonthlyReportRow {

    private String yearMonth;
    private int totalDays;
    private int successDays;
    private int monthlyLongestStreak;
    private String bestWeekday;
    private int earnedScore;
    private String weeklyResultsJson;
    private String difficultyResultsJson;
}

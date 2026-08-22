package com.kb.tangtang.report.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeMonthlyMissionRow {

    private LocalDate assignDate;
    private String result;
    private String missionType;
    private String difficultyName;
    private Integer difficultyScore;
    private boolean previousSuccess;
    private Long categoryId;
    private String categoryName;
    private BigDecimal baseAmount;
    private BigDecimal actualAmount;
}

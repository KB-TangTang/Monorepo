package com.kb.tangtang.report.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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
}

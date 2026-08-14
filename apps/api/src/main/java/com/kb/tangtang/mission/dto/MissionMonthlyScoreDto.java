package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissionMonthlyScoreDto {
    private String yearMonth;
    private int totalScore;
    private Integer topPercent;
}

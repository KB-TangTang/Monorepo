package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MissionMonthlyRankingDto {
    private String yearMonth;
    private int totalUsers;
    private List<MissionRankingEntryDto> topRankings;
    private MissionMyRankingDto myRanking;
}

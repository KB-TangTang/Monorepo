package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissionCertificateDto {
    private String yearMonth;
    private int totalUsers;
    private MissionMyRankingDto myRanking;
    private int streakDays;
    private int bestStreakDays;
    private int completedMissionCount;
    private int successMissionCount;
}

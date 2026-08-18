package com.kb.tangtang.mission.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionCertificateTitleInput {
    private final String yearMonth;
    private final int totalScore;
    private final int topPercent;
    private final int successMissionCount;
    private final int completedMissionCount;
    private final int streakDays;
    private final int bestStreakDays;
}

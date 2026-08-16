package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionCertificateStatsRow {
    private int streakDays;
    private int bestStreakDays;
    private int completedMissionCount;
    private int successMissionCount;
}

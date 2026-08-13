package com.kb.tangtang.mission.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MissionStreakDto {

    private long streakCount;
    private long longestStreakCount;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private List<WeeklyMissionResultDto> weeklyResults;
}

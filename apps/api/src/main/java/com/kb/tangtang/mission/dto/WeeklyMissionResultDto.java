package com.kb.tangtang.mission.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WeeklyMissionResultDto {

    private LocalDate date;
    private String result;
}

package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CategoryMissionStatus {

    private LocalDate assignDate;
    private String result;
}

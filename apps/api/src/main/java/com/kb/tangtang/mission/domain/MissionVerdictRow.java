package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MissionVerdictRow {

    private Long assignmentId;
    private String result;
    private LocalDate assignDate;
    private String categoryName;
    private BigDecimal currentAmount;
    private BigDecimal targetValue;
    private Integer basePoints;
    private Boolean previousSuccess;
}

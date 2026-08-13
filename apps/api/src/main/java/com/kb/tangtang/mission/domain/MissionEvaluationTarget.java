package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class MissionEvaluationTarget {
    private Long assignmentId;
    private Long userId;
    private LocalDate assignDate;
    private BigDecimal targetValue;
    private BigDecimal currentAmount;
}

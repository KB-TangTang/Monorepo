package com.kb.tangtang.mission.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RelativeMissionAssignment {
    private Long userId;
    private Long missionId;
    private Long difficultyId;
    private LocalDate assignDate;
    private BigDecimal targetRate;
    private BigDecimal targetValue;
    private BigDecimal baseAmount;
    private String assignmentReason;
}

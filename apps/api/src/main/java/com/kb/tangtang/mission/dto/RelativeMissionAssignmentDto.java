package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RelativeMissionAssignmentDto {
    private boolean assigned;
    private Long missionId;
    private String missionTitle;
    private Long categoryId;
    private LocalDate assignDate;
    private BigDecimal targetRate;
    private BigDecimal targetValue;
    private BigDecimal baseAmount;
    private String missionType;
    private String assignmentReason;
    private String guideMessage;

    public static RelativeMissionAssignmentDto skipped(LocalDate assignDate) {
        return RelativeMissionAssignmentDto.builder().assigned(false).assignDate(assignDate).build();
    }
}

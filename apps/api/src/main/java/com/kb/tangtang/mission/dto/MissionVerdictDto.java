package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MissionVerdictDto {

    private Long assignmentId;
    private String result;
    private LocalDate assignDate;
    private String categoryName;
    private BigDecimal currentAmount;
    private BigDecimal targetValue;
    private BigDecimal remainAmount;
    private BigDecimal overAmount;
    private Integer points;
    private Integer bonusPoints;
    private Integer streakDays;
    private Integer pendingCount;
    private List<MissionVerdictTransactionDto> transactions;
}

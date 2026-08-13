package com.kb.tangtang.mission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class TodayMissionDto {
    private Long missionId;
    private String missionTitle;
    private String missionContent;
    private String missionType;
    private Long categoryId;
    private String parentCategoryName;
    private String categoryName;
    private LocalDate assignDate;
    private String difficultyName;
    private BigDecimal targetRate;
    private BigDecimal baseAmount;
    private BigDecimal targetValue;
    private BigDecimal currentAmount;
    private String result;
    private String assignmentReason;
    private String guideMessage;
}

package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 상대형 미션 후보 카테고리 한 건. */
@Getter
@Builder
public class MissionCategoryRankDto {

    private int rank;
    private Long categoryId;
    private String parentCategoryName;
    private String categoryName;
    private BigDecimal totalAmount;
    private int transactionCount;
    private BigDecimal spendingRatio;
    private LocalDate latestMissionAssignDate;
    private String latestMissionResult;
}

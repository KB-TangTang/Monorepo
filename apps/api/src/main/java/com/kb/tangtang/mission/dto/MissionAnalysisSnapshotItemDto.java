package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 한 분석 주기에 포함된 카테고리 한 건. */
@Getter
@Builder
public class MissionAnalysisSnapshotItemDto {

    private Long categoryId;
    private String parentCategoryName;
    private String categoryName;
    private int categoryRank;
    private BigDecimal spendingAmount;
    private BigDecimal spendingRatio;
    private int transactionCount;
    private LocalDate assignedDate;
}

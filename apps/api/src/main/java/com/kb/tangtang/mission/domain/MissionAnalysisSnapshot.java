package com.kb.tangtang.mission.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** tbl_user_mission_analysis 한 행. 상위 소비 카테고리의 주기별 분석 스냅샷이다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionAnalysisSnapshot {

    private Long id;
    private Long userId;
    private LocalDate cycleStartDate;
    private Long categoryId;
    private String parentCategoryName;
    private String categoryName;
    private int categoryRank;
    private BigDecimal spendingAmount;
    private BigDecimal spendingRatio;
    private int transactionCount;
    private LocalDate assignedDate;
}

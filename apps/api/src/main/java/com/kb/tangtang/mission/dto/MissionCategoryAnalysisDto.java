package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 상대형 미션 배정을 위한 최근 소비 카테고리 분석 결과. */
@Getter
@Builder
public class MissionCategoryAnalysisDto {

    private LocalDate analysisStartDate;
    private LocalDate analysisEndDate;
    private int transactionCount;
    private int cumulativeTransactionCount;
    private int requiredCumulativeTransactionCount;
    private List<MissionCategoryRankDto> topCategories;
}

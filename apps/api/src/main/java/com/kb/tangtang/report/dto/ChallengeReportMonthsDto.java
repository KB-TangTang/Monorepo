package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 챌린지 리포트 진입 상태와 조회 가능한 확정 월 목록이다. */
@Getter
@Builder
public class ChallengeReportMonthsDto {

    private String entryState;
    private List<ChallengeReportMonthDto> months;
}

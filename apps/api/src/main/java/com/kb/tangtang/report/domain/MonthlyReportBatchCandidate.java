package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 월간 리포트 배치가 처리할 사용자 식별자다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportBatchCandidate {

    private Long userId;
}

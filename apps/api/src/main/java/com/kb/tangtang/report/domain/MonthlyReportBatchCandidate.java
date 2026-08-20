package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 월간 리포트 배치가 처리할 사용자와 리포트 제공 시점의 AI 활용 동의 상태다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportBatchCandidate {

    private Long userId;
    private boolean aiUsageConsented;
}

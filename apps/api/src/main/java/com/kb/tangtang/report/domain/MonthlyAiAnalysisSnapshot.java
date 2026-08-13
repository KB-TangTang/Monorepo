package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAiAnalysisSnapshot {

    private Long id;
    private String aiComment;
    private String compareComment;
    private String aiAnalysisStatus;
}

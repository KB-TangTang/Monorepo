package com.kb.tangtang.report.service;

import com.kb.tangtang.report.domain.MonthlyAiAnalysisInput;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;

public interface MonthlyAiAnalysisProvider {

    MonthlyAiAnalysisDto generate(MonthlyAiAnalysisInput input);
}

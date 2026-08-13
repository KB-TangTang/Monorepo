package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class MonthlyAiAnalysisResultReader {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;

    MonthlyAiAnalysisResultReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MonthlyAiAnalysisDto readCompleted(MonthlyAiAnalysisSnapshot snapshot, String yearMonth) {
        try {
            JsonNode feedbacksNode = objectMapper.readTree(snapshot.getAiComment());
            if (!feedbacksNode.isArray() || feedbacksNode.size() < 1 || feedbacksNode.size() > 3) {
                throw new IllegalArgumentException();
            }
            List<String> feedbacks = new ArrayList<>();
            for (JsonNode feedback : feedbacksNode) {
                if (!feedback.isTextual() || feedback.asText().trim().isEmpty()) {
                    throw new IllegalArgumentException();
                }
                feedbacks.add(feedback.asText().trim());
            }
            if (snapshot.getCompareComment() != null && snapshot.getCompareComment().isBlank()) {
                throw new IllegalArgumentException();
            }
            return MonthlyAiAnalysisDto.builder()
                    .yearMonth(yearMonth)
                    .status(STATUS_COMPLETED)
                    .feedbacks(feedbacks)
                    .savingsAnalogy(snapshot.getCompareComment())
                    .build();
        } catch (Exception ex) {
            throw new BusinessException("AI_ANALYSIS_RESULT_UNAVAILABLE",
                    "저장된 AI 분석 결과를 읽을 수 없어요. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}

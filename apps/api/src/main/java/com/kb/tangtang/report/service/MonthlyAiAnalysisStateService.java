package com.kb.tangtang.report.service;

import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 요청 전후의 상태 전이만 독립 트랜잭션으로 저장한다.
 * 외부 API 대기 시간 동안 DB 트랜잭션이나 행 잠금을 붙잡지 않게 별도 빈으로 둔다.
 */
@Service
public class MonthlyAiAnalysisStateService {

    private final MonthlyReportMapper monthlyReportMapper;

    public MonthlyAiAnalysisStateService(MonthlyReportMapper monthlyReportMapper) {
        this.monthlyReportMapper = monthlyReportMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claim(long userId, String yearMonth, String provider, String model,
                     String promptVersion, String inputHash) {
        return monthlyReportMapper.claimAiAnalysisGeneration(
                userId, yearMonth, provider, model, promptVersion, inputHash);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int complete(long userId, String yearMonth, String feedbacksJson, String savingsAnalogy) {
        return monthlyReportMapper.completeAiAnalysis(userId, yearMonth, feedbacksJson, savingsAnalogy);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(long userId, String yearMonth, String failureCode) {
        monthlyReportMapper.failAiAnalysis(userId, yearMonth, failureCode);
    }
}

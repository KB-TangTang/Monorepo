package com.kb.tangtang.report.mapper;

import com.kb.tangtang.report.domain.MonthlyCategorySpendingRow;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.domain.MonthlySpendingRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyReportMapper {

    LocalDate findUserCreatedDate(@Param("userId") long userId);

    BigDecimal sumNetSpending(@Param("userId") long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    List<MonthlySpendingRow> findMonthlySpending(@Param("userId") long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    List<MonthlyCategorySpendingRow> findMonthlyCategorySpending(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int countActiveFixedExpenseCandidates(@Param("userId") long userId);

    BigDecimal sumActiveTotalAssets(@Param("userId") long userId);

    BigDecimal sumLoanBalances(@Param("userId") long userId);

    int upsertPendingAiAnalysisSnapshot(@Param("userId") long userId,
                                        @Param("yearMonth") String yearMonth,
                                        @Param("totalAsset") BigDecimal totalAsset,
                                        @Param("totalDebt") BigDecimal totalDebt,
                                        @Param("netWorth") BigDecimal netWorth,
                                        @Param("categorySummaryJson") String categorySummaryJson);

    MonthlyAiAnalysisSnapshot findAiAnalysisSnapshot(@Param("userId") long userId,
                                                      @Param("yearMonth") String yearMonth);

    int claimAiAnalysisGeneration(@Param("userId") long userId,
                                  @Param("yearMonth") String yearMonth,
                                  @Param("provider") String provider,
                                  @Param("model") String model,
                                  @Param("promptVersion") String promptVersion,
                                  @Param("inputHash") String inputHash);

    int completeAiAnalysis(@Param("userId") long userId,
                           @Param("yearMonth") String yearMonth,
                           @Param("feedbacksJson") String feedbacksJson,
                           @Param("savingsAnalogy") String savingsAnalogy);

    int failAiAnalysis(@Param("userId") long userId,
                       @Param("yearMonth") String yearMonth,
                       @Param("failureCode") String failureCode);
}

package com.kb.tangtang.report.mapper;

import com.kb.tangtang.report.domain.MonthlyCategorySpendingRow;
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
}

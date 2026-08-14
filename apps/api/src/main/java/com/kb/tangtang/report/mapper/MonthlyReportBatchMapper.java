package com.kb.tangtang.report.mapper;

import com.kb.tangtang.report.domain.MonthlyReportBatchCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonthlyReportBatchMapper {

    List<MonthlyReportBatchCandidate> findEligibleCandidates(
            @Param("yearMonth") String yearMonth,
            @Param("targetMonthEnd") LocalDateTime targetMonthEnd,
            @Param("maxAutoAttempts") int maxAutoAttempts,
            @Param("retryNotBefore") LocalDateTime retryNotBefore);
}

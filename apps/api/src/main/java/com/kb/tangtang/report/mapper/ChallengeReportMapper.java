package com.kb.tangtang.report.mapper;

import com.kb.tangtang.report.domain.ChallengeMonthlyDifficultyPolicy;
import com.kb.tangtang.report.domain.ChallengeMonthlyMissionRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChallengeReportMapper {

    boolean hasActiveChallengeConsent(@Param("userId") long userId,
                                      @Param("now") LocalDateTime now);

    List<String> findConfirmedReportMonths(@Param("userId") long userId,
                                           @Param("currentYearMonth") String currentYearMonth);

    String findFirstMissionMonth(@Param("userId") long userId,
                                 @Param("currentMonthStart") LocalDate currentMonthStart);

    String findChallengeConsentMonth(@Param("userId") long userId,
                                     @Param("currentMonthStart") LocalDate currentMonthStart);

    List<Long> findFinalizedReportUserIds(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("yearMonth") String yearMonth);

    List<ChallengeMonthlyMissionRow> findFinalizedMissionRows(@Param("userId") long userId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    LocalDate findFirstMissionDate(@Param("userId") long userId,
                                   @Param("endDate") LocalDate endDate);

    List<ChallengeMonthlyDifficultyPolicy> findDifficultyPolicies();

    int upsertMonthlyReport(ChallengeMonthlyReportSnapshot snapshot);

    ChallengeMonthlyReportRow findMonthlyReport(@Param("userId") long userId,
                                                @Param("yearMonth") String yearMonth);

    ChallengeMonthlyReportRow findPreviousMonthlyReport(@Param("userId") long userId,
                                                        @Param("yearMonth") String yearMonth);
}

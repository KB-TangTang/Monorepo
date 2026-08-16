package com.kb.tangtang.report.mapper;

import com.kb.tangtang.report.domain.ChallengeMonthlyDifficultyPolicy;
import com.kb.tangtang.report.domain.ChallengeMonthlyMissionRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportSnapshot;
import com.kb.tangtang.report.dto.GroupRecordDto;
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
                                          @Param("yearMonth") String yearMonth,
                                          @Param("includeExisting") boolean includeExisting);

    List<ChallengeMonthlyMissionRow> findFinalizedMissionRows(@Param("userId") long userId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    LocalDate findFirstMissionDate(@Param("userId") long userId,
                                   @Param("endDate") LocalDate endDate);

    List<ChallengeMonthlyDifficultyPolicy> findDifficultyPolicies();

    int upsertMonthlyReport(ChallengeMonthlyReportSnapshot snapshot);

    int updateMonthlyGroupRecord(@Param("userId") long userId,
                                 @Param("yearMonth") String yearMonth,
                                 @Param("groupRecordJson") String groupRecordJson);

    List<Long> findMonthEndClosedGroupReportUserIds(@Param("endDate") LocalDate endDate,
                                                     @Param("yearMonth") String yearMonth);

    ChallengeMonthlyReportRow findMonthlyReport(@Param("userId") long userId,
                                                @Param("yearMonth") String yearMonth);

    ChallengeMonthlyReportRow findPreviousMonthlyReport(@Param("userId") long userId,
                                                        @Param("yearMonth") String yearMonth);

    /** CLOSED 그룹의 종료월 기준 전적만 집계한다. final_*와 CLOSED 전이는 challenge 모듈의 단일 트랜잭션이 보장한다. */
    GroupRecordDto findGroupRecord(@Param("userId") long userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}

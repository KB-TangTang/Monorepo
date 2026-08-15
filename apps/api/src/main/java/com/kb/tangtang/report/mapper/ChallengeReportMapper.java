package com.kb.tangtang.report.mapper;

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
}

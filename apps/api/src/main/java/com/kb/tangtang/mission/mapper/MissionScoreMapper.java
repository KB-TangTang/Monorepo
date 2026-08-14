package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface MissionScoreMapper {

    int calculateMonthlyScore(@Param("userId") long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    int upsertMonthlyScore(@Param("userId") long userId,
                           @Param("yearMonth") String yearMonth,
                           @Param("totalScore") int totalScore);

    Integer findMonthlyScore(@Param("userId") long userId,
                             @Param("yearMonth") String yearMonth);
}

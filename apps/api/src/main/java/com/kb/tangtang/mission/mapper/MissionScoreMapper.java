package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.MissionRankingRow;
import com.kb.tangtang.mission.domain.MissionCertificateStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

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

    List<MissionRankingRow> findTopRankings(@Param("yearMonth") String yearMonth,
                                            @Param("limit") int limit);

    MissionRankingRow findUserRanking(@Param("userId") long userId,
                                      @Param("yearMonth") String yearMonth);

    int countRankingUsers(@Param("yearMonth") String yearMonth);

    MissionCertificateStatsRow findCertificateStats(@Param("userId") long userId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    List<String> findRankingMonths();
}

package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.CategoryDailySpendingStats;
import com.kb.tangtang.mission.domain.MissionDifficulty;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.domain.RelativeMissionAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RelativeMissionAssignmentMapper {
    List<Long> findActiveUserIds();
    Long lockActiveUserDifficulty(@Param("userId") long userId);
    int countAssignment(@Param("userId") long userId, @Param("assignDate") LocalDate assignDate);
    MissionDifficulty findDifficulty(@Param("difficultyId") long difficultyId);
    List<MissionPoolItem> findRelativeMissions(@Param("categoryId") long categoryId,
                                               @Param("excludedMissionId") Long excludedMissionId);
    Long findLastMissionId(@Param("userId") long userId, @Param("categoryId") long categoryId);
    CategoryDailySpendingStats findDailySpendingStats(@Param("userId") long userId,
                                                      @Param("categoryId") long categoryId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
    int insertAssignment(RelativeMissionAssignment assignment);
}

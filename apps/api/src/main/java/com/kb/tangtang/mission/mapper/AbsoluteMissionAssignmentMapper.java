package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.CategorySpendingStats;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.domain.RelativeMissionAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AbsoluteMissionAssignmentMapper {
    boolean isRelativeMissionQualified(@Param("userId") long userId);
    Long lockActiveUserDifficulty(@Param("userId") long userId);
    int countAssignment(@Param("userId") long userId, @Param("assignDate") LocalDate assignDate);
    List<MissionPoolItem> findNoSpendAbsoluteMissions();
    CategorySpendingStats findCategorySpendingStats(@Param("userId") long userId,
                                                    @Param("categoryId") long categoryId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
    int insertAssignment(RelativeMissionAssignment assignment);
}

package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.MissionEvaluationTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MissionEvaluationMapper {

    List<Long> findPendingAssignmentIds(@Param("assignDate") LocalDate assignDate);

    MissionEvaluationTarget lockPendingAssignment(@Param("assignmentId") long assignmentId);

    int updateMissionResult(@Param("assignmentId") long assignmentId,
                            @Param("result") String result,
                            @Param("evaluatedAt") LocalDateTime evaluatedAt);

    int increaseSuccessStreak(@Param("userId") long userId,
                              @Param("checkedAt") LocalDateTime checkedAt);

    int resetStreak(@Param("userId") long userId,
                    @Param("checkedAt") LocalDateTime checkedAt);
}

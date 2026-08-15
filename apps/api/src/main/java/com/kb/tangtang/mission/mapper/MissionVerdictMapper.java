package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.MissionVerdictRow;
import com.kb.tangtang.mission.dto.MissionVerdictTransactionDto;
import com.kb.tangtang.mission.dto.WeeklyMissionResultDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MissionVerdictMapper {

    MissionVerdictRow findOldestUncheckedVerdict(@Param("userId") long userId,
                                                  @Param("assignDate") LocalDate assignDate);

    int countUncheckedVerdicts(@Param("userId") long userId,
                               @Param("assignDate") LocalDate assignDate);

    List<MissionVerdictTransactionDto> findVerdictTransactions(
            @Param("assignmentId") long assignmentId,
            @Param("userId") long userId);

    List<WeeklyMissionResultDto> findResultsThroughDate(
            @Param("userId") long userId,
            @Param("endDate") LocalDate endDate);

    int countOwnedFinalizedVerdict(@Param("assignmentId") long assignmentId,
                                   @Param("userId") long userId);

    int acknowledgeVerdict(@Param("assignmentId") long assignmentId,
                           @Param("userId") long userId,
                           @Param("checkedAt") LocalDateTime checkedAt);

    LocalDateTime findResultCheckedAt(@Param("assignmentId") long assignmentId,
                                      @Param("userId") long userId);
}

package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.dto.MissionStreakCountDto;
import com.kb.tangtang.mission.dto.WeeklyMissionResultDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TodayMissionMapper {

    TodayMissionDto findTodayMission(@Param("userId") long userId,
                                     @Param("assignDate") LocalDate assignDate);

    MissionStreakCountDto findStreakCounts(@Param("userId") long userId);

    List<WeeklyMissionResultDto> findWeeklyResults(@Param("userId") long userId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

}

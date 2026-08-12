package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.dto.TodayMissionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface TodayMissionMapper {

    TodayMissionDto findTodayMission(@Param("userId") long userId,
                                     @Param("assignDate") LocalDate assignDate);
}

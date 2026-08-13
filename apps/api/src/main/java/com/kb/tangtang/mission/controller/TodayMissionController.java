package com.kb.tangtang.mission.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.dto.MissionStreakDto;
import com.kb.tangtang.mission.service.TodayMissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class TodayMissionController {

    private final TodayMissionService todayMissionService;

    public TodayMissionController(TodayMissionService todayMissionService) {
        this.todayMissionService = todayMissionService;
    }

    @GetMapping("/today")
    public ApiResponse<TodayMissionDto> getTodayMission(@LoginUser Long userId) {
        return ApiResponse.ok(todayMissionService.getTodayMission(userId));
    }

    @GetMapping("/streak")
    public ApiResponse<MissionStreakDto> getMissionStreak(@LoginUser Long userId) {
        return ApiResponse.ok(todayMissionService.getMissionStreak(userId));
    }
}

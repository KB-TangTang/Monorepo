package com.kb.tangtang.mission.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.service.MissionScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionScoreController {

    private final MissionScoreService missionScoreService;

    public MissionScoreController(MissionScoreService missionScoreService) {
        this.missionScoreService = missionScoreService;
    }

    @GetMapping("/monthly-score")
    public ApiResponse<MissionMonthlyScoreDto> getCurrentMonthlyScore(@LoginUser Long userId) {
        return ApiResponse.ok(missionScoreService.getCurrentScore(userId));
    }
}

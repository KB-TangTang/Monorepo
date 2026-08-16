package com.kb.tangtang.mission.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionMonthlyRankingDto;
import com.kb.tangtang.mission.dto.MissionCertificateDto;
import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.dto.MissionRankingMonthsDto;
import com.kb.tangtang.mission.docs.MissionScoreControllerDocs;
import com.kb.tangtang.mission.service.MissionScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionScoreController implements MissionScoreControllerDocs {

    private final MissionScoreService missionScoreService;

    public MissionScoreController(MissionScoreService missionScoreService) {
        this.missionScoreService = missionScoreService;
    }

    @GetMapping("/monthly-score")
    public ApiResponse<MissionMonthlyScoreDto> getCurrentMonthlyScore(@LoginUser Long userId) {
        return ApiResponse.ok(missionScoreService.getCurrentScore(userId));
    }

    @GetMapping("/rankings")
    public ApiResponse<MissionMonthlyRankingDto> getMonthlyRanking(
            @LoginUser Long userId,
            @RequestParam(required = false) String yearMonth) {
        return ApiResponse.ok(missionScoreService.getMonthlyRanking(userId, yearMonth));
    }

    @GetMapping("/rankings/certificate")
    public ApiResponse<MissionCertificateDto> getCertificate(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(missionScoreService.getCertificate(userId, yearMonth));
    }

    @GetMapping("/rankings/months")
    public ApiResponse<MissionRankingMonthsDto> getRankingMonths(@LoginUser Long userId) {
        return ApiResponse.ok(missionScoreService.getRankingMonths());
    }
}

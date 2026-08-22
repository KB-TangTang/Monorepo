package com.kb.tangtang.mission.controller;

import com.kb.tangtang.mission.docs.MissionCategoryAnalysisControllerDocs;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.service.MissionAnalysisSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionCategoryAnalysisController implements MissionCategoryAnalysisControllerDocs {

    private final MissionAnalysisSnapshotService missionAnalysisSnapshotService;

    public MissionCategoryAnalysisController(MissionAnalysisSnapshotService missionAnalysisSnapshotService) {
        this.missionAnalysisSnapshotService = missionAnalysisSnapshotService;
    }

    @GetMapping("/categoryAnalysis")
    public ApiResponse<MissionCategoryAnalysisDto> getCategoryAnalysis(@LoginUser Long userId) {
        return ApiResponse.ok(missionAnalysisSnapshotService.getCurrentRotationAnalysis(userId));
    }
}

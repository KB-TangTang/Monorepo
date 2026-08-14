package com.kb.tangtang.mission.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.docs.MissionVerdictControllerDocs;
import com.kb.tangtang.mission.dto.MissionVerdictAcknowledgeDto;
import com.kb.tangtang.mission.dto.MissionVerdictDto;
import com.kb.tangtang.mission.service.MissionVerdictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions/verdicts")
public class MissionVerdictController implements MissionVerdictControllerDocs {

    private final MissionVerdictService service;

    public MissionVerdictController(MissionVerdictService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public ApiResponse<MissionVerdictDto> getPendingVerdict(@LoginUser Long userId) {
        return ApiResponse.ok(service.getPendingVerdict(userId));
    }

    @PostMapping("/{assignmentId}/acknowledge")
    public ApiResponse<MissionVerdictAcknowledgeDto> acknowledgeVerdict(
            @LoginUser Long userId,
            @PathVariable long assignmentId) {
        return ApiResponse.ok(service.acknowledge(userId, assignmentId));
    }
}

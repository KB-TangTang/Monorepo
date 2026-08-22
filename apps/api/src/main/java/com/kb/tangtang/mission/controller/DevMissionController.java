package com.kb.tangtang.mission.controller;

import com.kb.tangtang.mission.docs.DevMissionControllerDocs;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.service.DevMissionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/missions")
public class DevMissionController implements DevMissionControllerDocs {

    private final DevMissionService devMissionService;

    public DevMissionController(DevMissionService devMissionService) {
        this.devMissionService = devMissionService;
    }

    @PostMapping("/today/reassign")
    public ApiResponse<TodayMissionDto> reassignTodayMission(@LoginUser Long userId) {
        return ApiResponse.ok(devMissionService.reassignTodayMission(userId));
    }
}

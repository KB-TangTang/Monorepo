package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.docs.PersonalMissionUnlockControllerDocs;
import com.kb.tangtang.user.dto.PersonalMissionUnlockDto;
import com.kb.tangtang.user.dto.PersonalMissionUnlockSyncRequestDto;
import com.kb.tangtang.user.service.UserService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 맞춤 미션 개시 안내 상태 관리(이슈 #129). 상태가 tbl_user에 있어 user 모듈이 소유한다. */
@RestController
public class PersonalMissionUnlockController implements PersonalMissionUnlockControllerDocs {

    private final UserService userService;

    public PersonalMissionUnlockController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/main-challenge/mission-unlock/status")
    public ApiResponse<PersonalMissionUnlockDto> sync(
            @LoginUser Long userId,
            @RequestBody PersonalMissionUnlockSyncRequestDto request) {
        return ApiResponse.ok(userService.syncPersonalMissionUnlock(userId, request.getEnoughData()));
    }

    @PatchMapping("/api/main-challenge/mission-unlock/acknowledge")
    public ApiResponse<PersonalMissionUnlockDto> acknowledge(@LoginUser Long userId) {
        return ApiResponse.ok(userService.acknowledgePersonalMissionUnlock(userId));
    }
}

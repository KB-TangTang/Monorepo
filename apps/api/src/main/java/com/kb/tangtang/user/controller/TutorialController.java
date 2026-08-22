package com.kb.tangtang.user.controller;

import com.kb.tangtang.user.docs.TutorialControllerDocs;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.domain.TutorialType;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 튜토리얼 완료 처리 (이슈 #128).
 *
 * <p>경로는 챌린지 도메인인데 클래스는 user 패키지에 있다. 완료 시각이 사는 곳이
 * {@code tbl_user} 라서다 — 자기 테이블은 자기 모듈이 쓴다(apps/api/AGENTS.md 모듈 경계).
 * challenge 모듈이 이 값을 쓰려고 UserService 를 직접 부르게 두는 것보다 낫다.
 *
 * <p>경로 자체는 화면 기준으로 확정된 값이다. 메인 챌린지 쪽은
 * {@code PATCH /api/main-challenge/tutorial/complete} 로 등재돼 있다
 * (DECISIONS.md 2026-08-11 RV-108 종결).
 *
 * <p>{@code DELETE} 는 「튜토리얼 다시 보기」다. 완료 시각을 지우면 다음 진입에서 다시 뜬다 —
 * 마이페이지에 이미 있는 기능이라 완료 처리와 짝으로 둔다.
 */
@RestController
public class TutorialController implements TutorialControllerDocs {

    private final UserService userService;

    public TutorialController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/api/main-challenge/tutorial/complete")
    public ApiResponse<UserMeDto> completeMain(@LoginUser Long userId) {
        return ApiResponse.ok(userService.markTutorialSeen(userId, TutorialType.MAIN));
    }

    @DeleteMapping("/api/main-challenge/tutorial/complete")
    public ApiResponse<UserMeDto> resetMain(@LoginUser Long userId) {
        return ApiResponse.ok(userService.clearTutorialSeen(userId, TutorialType.MAIN));
    }

    @PatchMapping("/api/group-challenge/tutorial/complete")
    public ApiResponse<UserMeDto> completeGroup(@LoginUser Long userId) {
        return ApiResponse.ok(userService.markTutorialSeen(userId, TutorialType.GROUP));
    }

    @DeleteMapping("/api/group-challenge/tutorial/complete")
    public ApiResponse<UserMeDto> resetGroup(@LoginUser Long userId) {
        return ApiResponse.ok(userService.clearTutorialSeen(userId, TutorialType.GROUP));
    }
}

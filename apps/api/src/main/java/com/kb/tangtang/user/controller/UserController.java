package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.dto.UserNameUpdateRequestDto;
import com.kb.tangtang.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자 본인 정보.
 * JwtAuthInterceptor 를 통과한 요청만 도달하므로 userId 는 항상 채워져 있다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeDto> me(@LoginUser Long userId) {
        return ApiResponse.ok(userService.me(userId));
    }

    /**
     * 실명 저장. 간편인증 화면이 인증 요청 직전에 호출한다.
     *
     * 이름만 바꾸는 부분 갱신이라 PATCH 다. 같은 화면에서 받는 생년월일·통신사·휴대폰은
     * 여기로 오지 않는다 — 저장하지 않는 값이기 때문이다. (DECISIONS.md 2026-08-11 (4))
     */
    @PatchMapping("/me/name")
    public ApiResponse<UserMeDto> updateName(@LoginUser Long userId,
                                             @RequestBody UserNameUpdateRequestDto request) {
        return ApiResponse.ok(userService.updateName(userId, request.getName()));
    }
}

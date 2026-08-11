package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.dto.UserNameUpdateRequestDto;
import com.kb.tangtang.user.dto.UserNicknameUpdateRequestDto;
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

    /**
     * 닉네임(표시명) 설정·수정.
     *
     * 온보딩(AU_03_01)과 마이페이지(MY_01_03)가 **같은 엔드포인트**를 쓴다 —
     * 하는 일이 "닉네임을 이 값으로 바꾼다" 하나라 나눌 이유가 없다.
     * 온보딩 완료 여부는 `GET /api/users/me` 의 `nickname` 이 null 인지로 판별한다.
     * 별도 판별 API 를 두지 않는다. (API 연동규격 No.87)
     */
    @PatchMapping("/me/nickname")
    public ApiResponse<UserMeDto> updateNickname(@LoginUser Long userId,
                                                 @RequestBody UserNicknameUpdateRequestDto request) {
        return ApiResponse.ok(userService.updateNickname(userId, request.getNickname()));
    }
}

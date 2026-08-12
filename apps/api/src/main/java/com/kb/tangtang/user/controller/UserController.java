package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.dto.UserNameUpdateRequestDto;
import com.kb.tangtang.user.dto.UserNicknameUpdateRequestDto;
import com.kb.tangtang.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 로그인한 사용자 본인 정보.
 * JwtAuthInterceptor 를 통과한 요청만 도달하므로 userId 는 항상 채워져 있다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ImageProcessor imageProcessor;

    public UserController(UserService userService, ImageProcessor imageProcessor) {
        this.userService = userService;
        this.imageProcessor = imageProcessor;
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

    /**
     * 프로필 이미지 업로드 (multipart, 파트 이름은 `file` 고정).
     *
     * 닉네임과 엔드포인트를 나눈 이유: 온보딩에서 사진을 고르는 즉시 올라가야 하고
     * 마이페이지에서는 사진만 바꿀 수 있어야 한다. 하는 일이 다르면 엔드포인트도 나눈다.
     *
     * ⚠ **`file.getBytes()` 를 부르기 전에 크기부터 본다.** {@code ImageProcessor.toSquareJpeg}
     *   의 5MB 검사는 바이트 배열을 이미 메모리에 올린 뒤에야 동작해 상한이 할당 자체를
     *   막지 못한다 — {@code getSize()} 는 본문을 읽지 않고도 알 수 있으므로 여기서 먼저 막는다.
     */
    @PostMapping("/me/profile-image")
    public ApiResponse<UserMeDto> uploadProfileImage(
            @LoginUser Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        imageProcessor.requireWithinLimit(file.getSize());
        return ApiResponse.ok(userService.updateProfileImage(userId, file.getBytes()));
    }

    /** 프로필 이미지 삭제 — 기본(이니셜) 아바타로 되돌린다. 이미 없어도 성공이다. */
    @DeleteMapping("/me/profile-image")
    public ApiResponse<UserMeDto> deleteProfileImage(@LoginUser Long userId) {
        return ApiResponse.ok(userService.deleteProfileImage(userId));
    }
}

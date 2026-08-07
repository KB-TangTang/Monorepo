package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자 본인 정보.
 * JwtAuthInterceptor 를 통과한 요청만 도달하므로 userId 는 항상 채워져 있다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeDto> me(@LoginUser Long userId) {
        UserDto user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(UserMeDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .socialProvider(user.getSocialProvider())
                .build());
    }
}

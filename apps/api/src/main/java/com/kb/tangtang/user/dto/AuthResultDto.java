package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스 → 컨트롤러 전달용 묶음.
 * response 는 JSON 본문으로, refreshToken 은 Set-Cookie 로 나간다.
 */
@Getter
@AllArgsConstructor
@Builder
public class AuthResultDto {

    private LoginResponseDto response;
    private String refreshToken;
}

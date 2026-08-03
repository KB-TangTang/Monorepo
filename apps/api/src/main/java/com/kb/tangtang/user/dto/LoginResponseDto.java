package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/refresh 응답 본문.
 * 리프레시 토큰은 여기 담지 않는다 — httpOnly 쿠키로만 나간다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String accessToken;
    private UserMeDto user;
    /**
     * 필수 동의를 아직 하지 않은 사용자인지. 동의 화면은 후속 이슈라
     * 이번 범위에서는 프론트가 값을 저장만 하고 라우팅에 쓰지 않는다.
     */
    private boolean needsConsent;
}

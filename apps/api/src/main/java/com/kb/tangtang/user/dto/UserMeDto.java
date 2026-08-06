package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GET /api/users/me 응답. tbl_user 에 프로필 이미지 컬럼이 없으므로 포함하지 않는다.
 *
 * socialProvider 는 마이페이지 프로필 카드가 'google · 이메일' 을 그리는 데 쓴다.
 * /api/auth/refresh 응답에는 넣지 않는다 — 로그인 경로 전체에 영향을 주지 않기 위함이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMeDto {

    private Long id;
    private String nickname;
    private String email;
    private String socialProvider;
}

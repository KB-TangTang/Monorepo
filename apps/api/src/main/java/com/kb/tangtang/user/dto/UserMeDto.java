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
    /**
     * 실명(본인확인용). 간편인증 화면이 이 값을 입력창에 미리 채운다.
     * 아직 인증을 한 번도 안 한 사용자는 null 이다. (DECISIONS.md 2026-08-11 간편인증 이름 수정 허용)
     */
    private String name;
    private String email;
    private String socialProvider;
    /**
     * 튜토리얼 완료 시각. **NULL 이면 아직 안 봤다**는 뜻이라 프론트가 이 값만 보고 노출을 정한다.
     * 별도 조회 없이 판정하도록 로그인 응답(/api/auth/refresh)에도 함께 실린다. (이슈 #128)
     */
    private java.time.LocalDateTime tutorialSeenAt;
    private java.time.LocalDateTime groupTutorialSeenAt;
}

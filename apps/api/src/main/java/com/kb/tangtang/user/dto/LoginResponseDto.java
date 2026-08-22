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
    /*
     * ── 온보딩 게이트 3단 ────────────────────────────────
     * 프론트 라우터 가드가 부팅 시 이 응답 하나로 판정한다. 화면 진입마다 조회하지 않는다.
     * 순서는 동의 → 금융동의 → 계좌연동 → 닉네임이고, 마지막 단계인 닉네임은
     * user.nickname 이 null 인지로 본다(별도 플래그를 두지 않는다).
     * (DECISIONS.md 2026-08-11 (7))
     */

    /** SIGNUP 필수 동의(약관·개인정보·금융정보) 미완료. */
    private boolean needsConsent;

    /**
     * `THIRD_PARTY`(CODEF 제3자 제공) 동의 미완료.
     *
     * ⚠ 이 항목을 `needsConsent` 에 합치면 안 된다 — 그러면 계좌를 아직 연동하지 않은
     * 사용자가 가입 동의 화면을 영원히 벗어나지 못한다. 그래서 단계를 나눈다.
     */
    private boolean needsFinancialConsent;

    /**
     * 활성 연결 계좌가 0개.
     *
     * 계좌가 없으면 고정지출 탐지·미션·리포트가 전부 빈 화면이라 홈에 보내도 할 일이 없다.
     * 그래서 온보딩에서 계좌 연동을 강제한다.
     */
    private boolean needsAccountLink;
}

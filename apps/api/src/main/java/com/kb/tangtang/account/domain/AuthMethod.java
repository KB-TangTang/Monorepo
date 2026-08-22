package com.kb.tangtang.account.domain;

/**
 * 계좌 연동 인증 수단.
 *
 * 어느 수단을 쓸지는 서버가 정해 프론트에 내려준다(GET /api/accounts/link/auth-methods).
 * 프론트는 mock/codef 를 알지 못하며, 전환은 financial.client 프로퍼티 한 줄로 끝난다.
 * (DECISIONS.md 2026-08-05 계좌 연동 항목)
 */
public enum AuthMethod {

    /** 마이데이터형 간편인증. 인증 한 번으로 선택한 기관을 함께 연결한다. */
    SIMPLE_AUTH,

    /** 기관별 아이디·비밀번호 로그인. 실 CODEF 스크래핑 경로다. */
    INSTITUTION_LOGIN
}

package com.kb.tangtang.account.domain;

/** 인증 진행 상태. 프론트가 1초 간격으로 폴링한다. */
public enum AuthStatus {

    /** 사용자의 승인을 기다리는 중 */
    PENDING,

    /** 승인 완료. 계좌 조회를 시작할 수 있다 */
    APPROVED,

    /** 사용자가 거절했거나 인증기관이 실패를 돌려줬다 */
    FAILED,

    /** 제한 시간을 넘겼다 */
    EXPIRED
}

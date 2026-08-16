package com.kb.tangtang.challenge.chat.domain;

/**
 * 재판 봇이 남기는 시스템 메시지의 종류 (이슈 #174).
 *
 * <p>화면이 카드 모양을 고르는 기준이다. 문구만으로는 "기소 카드" 와 "판결 카드" 를 구분할 수 없어
 * 프론트가 문자열을 파싱하게 되는데, 그러면 문구를 한 글자만 고쳐도 화면이 조용히 깨진다.
 *
 * <p>참여자 메시지(TEXT)에는 이 값이 없다(null).
 */
public enum ChatSystemType {

    /** 소비 위반 적발 — 아직 재판 전이다 */
    VIOLATION_DETECTED,

    /** 재판 개시 */
    TRIAL_OPENED,

    /** 변론 등록 */
    DEFENSE_REGISTERED,

    /** 판결 확정 — 화면이 도장을 찍는다 */
    VERDICT_CONFIRMED
}

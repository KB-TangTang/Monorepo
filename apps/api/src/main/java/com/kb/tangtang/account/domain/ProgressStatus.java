package com.kb.tangtang.account.domain;

/** 기관별 계좌 조회 진행 상태. 프론트 진행 화면의 행 상태와 값이 같다. */
public enum ProgressStatus {

    /** 아직 차례가 오지 않음 */
    WAITING,

    /** 조회 중 */
    FETCHING,

    /** 조회 완료 */
    DONE,

    /** 조회 실패. 나머지 기관은 계속 진행한다 */
    FAILED
}

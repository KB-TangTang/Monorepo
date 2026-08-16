package com.kb.tangtang.report.dto;

/** 선택 월의 그룹 전적 표시 상태. */
public enum GroupRecordState {

    /** 종료한 그룹 중 최종 판정 대기 상태가 있어 전적을 아직 표시할 수 없다. */
    JUDGING,

    /** 선택 월에 종료되고 최종 확정된 그룹 전적이 있다. */
    READY,

    /** 선택 월에 표시할 그룹 전적이 없다. */
    EMPTY
}

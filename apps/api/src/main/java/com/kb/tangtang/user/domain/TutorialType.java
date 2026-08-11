package com.kb.tangtang.user.domain;

/**
 * 튜토리얼 종류. 완료 시각을 어느 컬럼에 남길지 정한다.
 *
 * 둘 다 tbl_user 에 있다 — 동의 테이블에 끼우면 동의 철회 API 가 튜토리얼 플래그까지
 * 건드릴 수 있는 구조가 되기 때문이다. (DECISIONS.md 2026-08-11 RV-108 종결)
 *
 * ⚠ 이 이름은 매퍼 XML 의 CASE 식이 문자열로 비교한다. 바꾸면 XML 도 같이 고쳐야 한다.
 */
public enum TutorialType {

    /** 메인(개인·대법원) 챌린지 튜토리얼 MC_01_05 → tbl_user.tutorial_seen_at */
    MAIN,

    /** 그룹(지방법원) 챌린지 튜토리얼 GC_01_01 → tbl_user.group_tutorial_seen_at */
    GROUP
}

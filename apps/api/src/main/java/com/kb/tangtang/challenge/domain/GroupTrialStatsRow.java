package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 종료 그룹의 재판 전적 한 벌 (이슈 #173). 종료 화면 「재판 기록」 링크의 숫자다.
 *
 * <p>확정(GUILTY·INNOCENT)된 재판만 센다. 진행 중 배지를 세는
 * {@link GroupTrialSummaryRow} 와 반대 방향이라 행을 따로 뒀다 — 종료 그룹에는
 * 진행 중 재판이 없고, 진행 중 그룹의 전적은 화면이 쓰지 않는다.
 */
@Getter
@Setter
public class GroupTrialStatsRow {

    /** 확정된 재판 수 = 유죄 + 무죄. */
    private int totalTrials;

    private int guiltyCount;

    private int innocentCount;
}

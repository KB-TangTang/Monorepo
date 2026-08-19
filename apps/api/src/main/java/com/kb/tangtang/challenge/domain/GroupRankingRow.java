package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 명예 법정(생존자 랭킹) 한 줄의 원재료 (이슈 #173).
 *
 * <p>진행 중({@code findRankingActive})·종료({@code findRankingClosed}) 두 조회가 같은 행을 쓴다.
 * 진행 중에는 {@code finalOutcome}·{@code finalChargeAmount} 가 NULL 이다 — 확정 전이라 값 자체가
 * 없고, 화면도 이 NULL 을 「아직 탈락 아님」의 근거로 쓴다.
 */
@Getter
@Setter
public class GroupRankingRow {

    /**
     * 순위. 진행 중에는 SQL 의 {@code RANK()} 가, 종료 후에는 {@code final_rank} 저장값이 채운다.
     * 동률은 공동 순위다 (1, 1, 3).
     */
    private Integer rank;

    private Long userId;

    private String nickname;

    /** 원본 키다. URL 변환은 서비스가 {@code ImageStorage} 로 한다. */
    private String profileImageKey;

    /** 남은 목숨. 일일평가 화면의 목숨 아이콘·정렬 1차 기준이다. */
    private Integer livesCount;

    /**
     * 누적 소비액(표시용). 무죄 감액이 반영된 값이다.
     *
     * <p>종료 후에도 내려간다 — 순위의 근거는 {@code final_*} 저장값이고 이 값은 화면 표시 전용이다.
     * 집계 행이 없는 참여자도 0 으로 내려온다.
     */
    private BigDecimal totalConsumption;

    /** {@code SURVIVED}/{@code ELIMINATED}. 종료(CLOSED) 조회에서만 채워진다. */
    private String finalOutcome;

    /** 최종 실제 부담금. 종료(CLOSED) 조회에서만 채워진다. */
    private BigDecimal finalChargeAmount;
}

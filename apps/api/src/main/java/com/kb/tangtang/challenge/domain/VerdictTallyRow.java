package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 개표 대상 기소 한 건 (이슈 #172).
 *
 * <p>{@code VOTING} 인 기소 중 「투표 마감이 지났거나 투표할 사람이 전부 던진」 건을
 * {@link com.kb.tangtang.challenge.mapper.IndictmentMapper#findVotingToTally} 가 이 모양으로 준다.
 *
 * <p><b>한 문장으로 다 읽는 이유</b> — 동률이면 판사 탕이(LLM)에게 넘겨야 하고, 그때 필요한
 * 기소 사유·한도·소비액·변론 원문이 전부 여기 있어야 한다. 동률일 때만 다시 조회하면
 * 배치가 재판마다 쿼리를 세 번 더 던지고, 그 사이에 값이 바뀌면 표 분포와 판결 근거가
 * 서로 다른 시점을 가리키게 된다.
 *
 * <p>표 분포는 <b>확정 시점의 스냅샷</b>이다. 개표 이후에는 투표가 들어올 수 없으므로
 * ({@code status} 가 더 이상 {@code VOTING} 이 아니다) 값이 어긋날 여지는 없다.
 */
@Getter
@Setter
public class VerdictTallyRow {

    private Long indictmentId;

    private Long groupId;

    /** 피고. 유죄면 이 사람의 목숨이 깎인다. */
    private Long userId;

    /** 감액을 적을 {@code tbl_group_challenge_daily_result} 행. */
    private Long resultId;

    /** 피고 닉네임. 채팅 시스템 메시지 문구에 쓴다. */
    private String nickname;

    /* ── 판사 탕이에게 넘길 사건 개요 (동률일 때만 쓰인다) ────────── */

    /** 기소 문구. 평가 배치가 만든 한 줄이다. */
    private String message;

    /** {@link EvalType} 의 이름. */
    private String evalType;

    /** NULL 이면 총소비 챌린지다. */
    private String categoryName;

    private BigDecimal limitAmount;

    /** 결산 구간의 소비액(무죄 감액 반영). */
    private BigDecimal currentAmount;

    /** {@code currentAmount - limitAmount}. */
    private BigDecimal exceededAmount;

    /* ── 변론 (없으면 전부 NULL) ─────────────────────────────── */

    /** 변론 본문. 변론 없이 마감된 재판은 NULL 이다 — 그래도 투표는 그대로 진행된다. */
    private String defenseContent;

    /** 피고가 신고한 실제 개인 부담금. */
    private BigDecimal actualBurdenAmount;

    /**
     * 무죄 확정 시 소비액에서 빼 줄 금액. <b>#170 이 서버 계산으로 이미 저장해 둔 값</b>이라
     * 여기서 다시 계산하지 않는다 ({@code DefenseService#registerDefense}).
     *
     * <p>변론이 없으면 NULL 이고, 그때는 무죄여도 감액이 없다.
     */
    private BigDecimal deductionAmount;

    /* ── 표 분포 ────────────────────────────────────────────── */

    private int guiltyCount;

    private int innocentCount;

    /** 참여자 - 피고 1명. 피고 혼자인 그룹이면 0 이다. */
    private int totalVoters;
}

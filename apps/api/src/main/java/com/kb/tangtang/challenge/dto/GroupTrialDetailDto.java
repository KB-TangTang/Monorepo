package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 재판 상세 한 벌 (이슈 #170).
 *
 * <p><b>이슈 #171(투표)과 공유하는 계약이다.</b> 기소 안내 · 변론 작성 · 투표 · 판결 상세가
 * 모두 이 응답 하나를 쓴다. 화면마다 엔드포인트를 두면 같은 값을 네 곳에서 다르게 계산하게 된다.
 *
 * <p>투표 필드({@code myVerdict} · {@code voteCount} · {@code totalVoters})도 #170 이 채운다.
 * {@code IndictmentMapper.xml} 에 서브쿼리가 이미 있어 비용이 들지 않는다 —
 * #171 은 <b>투표를 쓰는 쪽</b>만 만들면 된다.
 *
 * <p><b>결산 구간의 거래 목록은 여기 없다.</b> 상세는 투표자도 보지만 거래 목록은 피고 본인만
 * 본다. 한 응답에 섞으면 투표자에게 남의 거래내역이 흘러간다 —
 * {@code GET .../trials/{indictmentId}/transactions} 로 분리했다.
 */
@Getter
@Builder
public class GroupTrialDetailDto {

    private Long indictmentId;

    private Long groupId;

    private String groupName;

    /** {@code DEFENSE_WAIT} · {@code VOTING} · {@code GUILTY} · {@code INNOCENT}. */
    private String status;

    /** 1=유죄 / 0=무죄 / NULL=미확정. */
    private Boolean result;

    /** {@code VOTE} · {@code NO_VOTE} · {@code AI_JUDGMENT} · {@code CONFESSION}. 확정 전 NULL. */
    private String verdictMethod;

    /**
     * 판사 탕이의 판결 사유 원문 (이슈 #172). {@code verdictMethod} 가 {@code AI_JUDGMENT} 일
     * 때만 값이 있다. 화면은 이 문장을 그대로 띄운다 — 요약하지 않는다.
     *
     * <p><b>개표 전에는 NULL</b> 이다. {@link #guiltyCount} 와 같은 분기에서 가린다 —
     * AI 판결은 동률일 때만 나오므로 사유가 보이는 것만으로 표 분포가 드러난다.
     */
    private String aiVerdictReason;

    /** 기소 문구. */
    private String message;

    private TrialAccusedDto accused;

    /* ── 결산 구간 ─────────────────────────────────── */
    /** {@code DAILY} · {@code PERIOD}. 화면이 "일일결산 / 기간결산" 문구를 이 값으로 고른다. */
    private String evalType;

    /** 일일평가에서 위반한 날짜. */
    private LocalDate challengeDate;

    /** 기간평가 구간. 일일평가에서도 그룹 기간이라 값이 있다. */
    private LocalDate startDate;
    private LocalDate endDate;

    /** NULL 이면 총소비 챌린지다. */
    private Long categoryId;
    private String categoryName;

    /* ── 금액 ─────────────────────────────────────── */
    private BigDecimal limitAmount;

    /** 결산 구간의 소비액(무죄 감액 반영). <b>실제 부담금 입력의 상한</b>이다. */
    private BigDecimal currentAmount;

    /** {@code currentAmount - limitAmount}. */
    private BigDecimal exceededAmount;

    /* ── 시각 ─────────────────────────────────────── */
    private LocalDateTime createdAt;

    /**
     * 변론 마감 = {@code createdAt + challenge.trial.defense-hours}.
     * <b>컬럼이 아니라 계산값이다</b> — {@code tbl_indictment} 에 마감 컬럼을 두지 않기로 했다
     * ({@code db/schema.sql} tbl_indictment 주석).
     */
    private LocalDateTime defenseDeadline;

    /** 투표 마감 = 변론 마감 + {@code challenge.trial.vote-hours}. */
    private LocalDateTime voteDeadline;

    /* ── 변론 · 투표 ─────────────────────────────────── */
    /** 아직 변론이 없으면 NULL. */
    private TrialDefenseDto defense;

    /** 내가 던진 표. 안 던졌으면 NULL. 피고 본인은 항상 NULL 이다. */
    private String myVerdict;

    private int voteCount;

    /** 참여자 - 피고 1명. */
    private int totalVoters;

    /**
     * 유죄 표수. <b>개표 전에는 NULL</b> 이다 — 투표 중에 비율이 보이면 편승 투표가 생긴다.
     *
     * <p>{@code int} 가 아니라 {@code Integer} 인 이유가 여기 있다. {@code int} 면 가릴 때 0 이
     * 되어 화면이 「아직 모른다」와 「0표」를 구분할 수 없다.
     */
    private Integer guiltyCount;

    /** 무죄 표수. {@link #guiltyCount} 와 같은 규칙이다. */
    private Integer innocentCount;

    /**
     * 익명 한줄 코멘트 목록 (오래된 순). <b>개표 전에는 NULL</b> 이다.
     *
     * <p>코멘트 문장에는 유무죄가 그대로 드러나므로 투표 중에 내려보내면
     * {@code guiltyCount} 를 가린 의미가 없어진다.
     */
    private List<TrialVoteCommentDto> comments;
}

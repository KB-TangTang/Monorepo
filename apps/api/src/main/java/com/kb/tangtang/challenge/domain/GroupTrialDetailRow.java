package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 재판 상세 한 벌의 원재료 (이슈 #170).
 *
 * <p>기소 · 그룹 · 피고 · 투표 현황을 한 쿼리로 받는다. 변론 본문·증빙은 {@code tbl_defense} 를
 * 따로 읽는다 — 조인하면 이미지 개수만큼 행이 늘어나 나머지 컬럼이 전부 중복된다.
 *
 * <p><b>마감 컬럼이 없다.</b> {@code tbl_indictment} 에 마감 시각이 없어 {@link #createdAt} 에
 * {@code challenge.trial.*} 시간을 더해 서비스가 계산한다.
 */
@Getter
@Setter
public class GroupTrialDetailRow {

    private Long indictmentId;

    private Long groupId;

    private String groupName;

    /** {@link IndictmentStatus} 의 이름. */
    private String status;

    /** 1=유죄 / 0=무죄 / NULL=미확정. {@code status} 와 같은 정보다. */
    private Boolean result;

    /** {@link VerdictMethod} 의 이름. 확정 전에는 NULL. */
    private String verdictMethod;

    /** 기소 문구. 배치가 만든 한 줄이다. */
    private String message;

    /* ── 피고 ─────────────────────────────────────── */
    private Long userId;

    private String nickname;

    /** 원본 키다. URL 변환은 서비스가 {@code ImageStorage} 로 한다. */
    private String profileImageKey;

    /* ── 결산 구간 ─────────────────────────────────── */
    /** {@link EvalType} 의 이름. */
    private String evalType;

    /** 일일평가에서 위반한 날짜. 기간평가에서는 {@code end_date} 와 같다. */
    private LocalDate challengeDate;

    private LocalDate startDate;

    private LocalDate endDate;

    /** NULL 이면 총소비 챌린지다. */
    private Long categoryId;

    private String categoryName;

    /* ── 금액 ─────────────────────────────────────── */
    private BigDecimal limitAmount;

    /** 결산 구간의 소비액(무죄 감액 반영). 변론 화면의 부담금 상한이다. */
    private BigDecimal currentAmount;

    /** {@code currentAmount - limitAmount}. 음수일 수 없다(초과했을 때만 기소된다). */
    private BigDecimal exceededAmount;

    /* ── 투표 현황 ──────────────────────────────────── */
    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 안 던졌으면 NULL. */
    private String myVerdict;

    private int voteCount;

    /** 참여자 - 피고 1명. */
    private int totalVoters;

    /** 마감 계산의 기준점. */
    private LocalDateTime createdAt;
}

package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 그룹 챌린지 목록 아이템. 시작 전 · 진행 중 · 종료됨 세 탭이 같은 형태를 쓴다.
 *
 * 탭마다 의미 없는 필드는 NULL 로 내려간다.
 *   - 시작 전: {@code daysUntilStart} 사용, {@code currentDay} NULL
 *   - 진행 중: {@code currentDay}·{@code livesCount} 사용
 *   - 종료됨: {@code finalOutcome}·{@code finalRank}·{@code finalChargeAmount} 사용
 *
 * 재판 관련 필드({@code pendingTrialCount} 등)는 {@code tbl_indictment} 구현 전이라
 * 항상 0/NULL 이다. 절감액과 채팅 필드는 근거 데이터가 없어 아예 내려주지 않는다.
 */
@Getter
@Builder
public class ChallengeGroupSummaryDto {

    /* ── tbl_challenge_group ─────────────────────────── */
    private Long id;
    private Long adminId;
    private String groupName;
    private Long categoryId;
    private String categoryName;
    private Integer limitAmount;
    private String evalType;
    private Integer maxMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    private String status;

    /* ── tbl_group_member (로그인 사용자 본인) ────────── */
    private Integer livesCount;
    private String finalOutcome;
    private Integer finalRank;
    private BigDecimal finalChargeAmount;

    /* ── 파생 ─────────────────────────────────────────── */
    /** 시작일·종료일을 포함한 기간 일수. */
    private int totalDays;
    /** 진행 중일 때의 경과 일차(1부터). 시작 전·종료 후에는 NULL. */
    private Integer currentDay;
    /** 시작까지 남은 일수. 시작일 도래 후에는 NULL. */
    private Integer daysUntilStart;
    /** 평가 주기가 결정하는 목숨 상한. DAILY=기간 일수, PERIOD=1. */
    private int maxLives;
    private int memberCount;
    private boolean owner;

    private List<GroupMemberDto> members;

    /* ── 재판 (후속 이슈 전까지 고정값) ──────────────── */
    /** 내가 투표해야 할 재판 수. tbl_indictment 구현 전까지 0. */
    private int pendingTrialCount;
    /** 내가 피고인인 기소가 있는지. tbl_indictment 구현 전까지 false. */
    private boolean defendant;
}

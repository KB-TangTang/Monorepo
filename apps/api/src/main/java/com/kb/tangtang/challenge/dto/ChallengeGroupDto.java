package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API 가 내보내는 그룹 챌린지 한 건. 목록 · 상세 · 초대 코드 미리보기가 모두 이 모양을 쓴다.
 *
 * <p>목록과 상세를 다른 DTO 로 나누지 않았다. 컬럼 차이가 {@code memo} 하나뿐인데
 * 나누면 미리보기 응답이 {@code challenge.challenge.groupName} 처럼 두 겹으로 접힌다.
 * 그룹은 한 명당 많아야 수십 건이라 메모를 목록에 함께 실어도 부담이 없다.
 *
 * <p>탭마다 의미 없는 필드는 NULL 로 내려간다.
 * <ul>
 *   <li>시작 전: {@code daysUntilStart} · {@code currentDay} 는 NULL</li>
 *   <li>진행 중: {@code currentDay} · {@code livesCount}</li>
 *   <li>종료됨: {@code finalOutcome} · {@code finalRank} · {@code finalChargeAmount}</li>
 * </ul>
 *
 * <p>표시용 이니셜 · 절감액 · 채팅 필드는 없다. 이니셜은 닉네임에서 프론트가 만들고,
 * 나머지 둘은 아직 근거 데이터가 없다.
 */
@Getter
@Builder
public class ChallengeGroupDto {

    /* ── tbl_challenge_group ─────────────────────────── */
    private Long id;
    private Long adminId;
    private String groupName;
    private Long categoryId;
    /** 조인 파생. {@code categoryId} 가 NULL(총소비)이면 NULL. */
    private String categoryName;
    private Integer limitAmount;
    private String evalType;
    private Integer maxMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    private String status;
    /** 리워드·벌칙 자유 메모. 표시 전용이며 시스템이 집행하지 않는다. */
    private String memo;

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

    /** 로그인 사용자가 방장인지. */
    private boolean owner;
    /** 로그인 사용자가 참여자인지. 초대 코드 미리보기에서는 대개 false 다. */
    private boolean member;
    /** 지금 참여할 수 있는지 (미참여 · 모집 중 · 정원 여유). */
    private boolean joinable;

    private List<GroupMemberDto> members;

    /* ── 재판 (tbl_indictment 구현 전까지 고정값) ────── */
    /** 내가 투표해야 할 재판 수. 항상 0. */
    private int pendingTrialCount;
    /** 내가 피고인인 기소가 있는지. 항상 false. */
    private boolean defendant;
}

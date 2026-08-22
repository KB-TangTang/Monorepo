package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * <p>표시용 이니셜 · 절감액은 없다. 이니셜은 닉네임에서 프론트가 만들고,
 * 절감액은 아직 근거 데이터가 없다.
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
    /** 그룹을 만든 시각. 초대 화면의 소환장이 「발부일」로 찍는다. */
    private LocalDateTime createdAt;

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

    /* ── 채팅 요약 (Redis 파생) ───────────────────────
     *
     * 목록 카드와 상세 FAB 배지가 함께 쓴다. 중첩 {@code chat} 객체를 만들지 않고 평평하게 둔 것은
     * 목록 카드가 이미 이 세 이름을 읽고 있어서다.
     *
     * <p><b>참여자에게만 채운다.</b> 초대 코드 미리보기도 이 DTO 를 쓰기 때문에, 무조건 채우면
     * 코드만 아는 비참여자에게 방의 마지막 대화가 새어 나간다.
     */
    /** 내가 아직 안 읽은 메시지 수. 비참여자 · 대화 없음 · Redis 실패는 모두 0. */
    private int unreadChatCount;
    /** 가장 최근 메시지 본문. 시스템 메시지도 그대로 보여준다(카카오톡과 같은 방식). 없으면 NULL. */
    private String lastChatMessage;
    /** 가장 최근 메시지 시각. 표시 형식은 프론트가 정한다. 없으면 NULL. */
    private LocalDateTime lastChatTime;

    /* ── 재판 배지 (목록 전용) ───────────────────────── */
    /*
     * 세 필드는 「내가 지금 해야 할 일」을 가리킨다. 재판이 열려 있는지가 아니다.
     * 남의 재판이 진행 중이어도 내가 이미 투표했으면 「투표완료」다 (GroupActiveCard.vue).
     *
     * ⚠ 목록(GC_01_01)에서만 채워진다. 상세·참여·초대 미리보기 응답에서는 0 · false · null 이다 —
     *   상세 화면은 같은 응답의 indictments 로 판단하므로 같은 것을 두 번 세지 않는다.
     */

    /** 투표 중인데 내가 아직 안 던진 기소 수. 0 보다 크면 「투표중」. */
    private int pendingTrialCount;

    /** 내가 피고이고 아직 변론을 안 낸 기소가 있는지. 참이면 「변론필요」 — 배지 중 가장 급하다. */
    private boolean defendant;

    /**
     * 내 투표 진행 상태. {@code PENDING}(아직 안 던진 표가 있음) · {@code DONE}(던졌고 개표 대기) ·
     * NULL(투표할 재판 없음).
     *
     * <p>{@code pendingTrialCount} 로 갈음할 수 없다. 둘 다 0 이어도 <b>투표를 마쳐서</b> 0 인 것과
     * <b>재판이 없어서</b> 0 인 것은 화면에서 「투표완료」와 「순항중」으로 갈린다.
     */
    private String myVoteStatus;
}

package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 그룹 상세의 재판 카드 한 장 (이슈 #169).
 *
 * <p>진행 중인 기소({@code DEFENSE_WAIT} · {@code VOTING})만 담는다. 확정된 기소는
 * 카드가 아니라 전적으로 간다.
 *
 * <p><b>카드 종류는 서버가 정하지 않는다.</b> 프론트가 {@code mine} · {@code status} ·
 * {@code myVote} 세 값을 조합해 「변론 필요 / 변론 제출됨 / 투표 필요 / 투표 완료」를 만든다
 * ({@code GroupDetailTrialCarousel.vue}). 판정을 서버로 올리면 카드 종류가 하나 늘 때마다
 * war 를 다시 올려야 한다.
 *
 * <p>마감 두 개를 <b>둘 다</b> 내려보낸다. 화면이 {@code status} 로 골라 쓴다 —
 * 상태별로 한 필드만 채우면 상태 전이 배치가 도는 순간 화면에서 마감이 잠깐 사라진다.
 */
@Getter
@Builder
public class GroupIndictmentDto {

    /** 기소 ID. 변론·투표 화면으로 넘어갈 때 쓰는 키다. */
    private Long id;

    /* ── 피고 ─────────────────────────────────────────── */
    private Long userId;
    private String nickname;
    /** 저장된 키가 아니라 조립된 URL 이다. 프로필을 안 올렸으면 NULL — 화면이 이니셜을 그린다. */
    private String profileImageUrl;

    /** {@code DEFENSE_WAIT} · {@code VOTING}. */
    private String status;

    /**
     * 위반한 날짜. 기소 생성일이 아니다 — 심야 거래를 다음 날 배치가 잡아 하루 어긋난다.
     * 「8월 5일 결산」의 그 날짜다.
     */
    private LocalDate settlementDate;

    /** 한도 초과액. */
    private BigDecimal exceededAmount;

    /* ── 로그인 사용자 기준 ───────────────────────────── */
    /** 내가 피고인지. JSON 키는 {@code mine} 이다(Lombok 이 {@code isMine()} 을 만든다). */
    private boolean mine;
    /** 피고가 변론을 냈는지. 카드가 「변론 제출」로 바뀌는 조건이다. */
    private boolean defended;
    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 아직 안 던졌으면 NULL. */
    private String myVote;

    private int voteCount;
    /** 참여자 - 피고 1명. */
    private int totalVoters;

    /* ── 마감 (저장값이 아니라 계산값) ────────────────── */
    private LocalDateTime defenseDeadline;
    private LocalDateTime voteDeadline;
}

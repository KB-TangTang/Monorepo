package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 확정된 재판 기록 한 줄.
 *
 * <p>두 화면이 같은 모양을 쓴다 — 그룹 하나의 재판 기록과 지방법원 홈의 「지난 재판」 전체 목록.
 * 확정된 기소({@code GUILTY} · {@code INNOCENT})만 담는다. 진행 중인 기소는
 * {@link GroupIndictmentDto} 가 받는다.
 *
 * <p><b>{@link GroupIndictmentDto} 와 합치지 않는다.</b> 저쪽의 축은 마감(카운트다운)이고
 * 여기는 확정 시각·개표 결과다. 한 DTO 로 묶으면 진행 중에는 개표가, 확정 후에는 마감이
 * 항상 NULL 인 필드가 절반씩 생긴다.
 *
 * <p><b>개표 결과를 가리지 않는다.</b> {@code GroupTrialService#findTrialDetail} 이 투표 중에
 * {@code guiltyCount} 를 NULL 로 덮는 것은 편승 투표를 막기 위함인데(이슈 #171), 그 이유는
 * 투표가 열려 있을 때만 성립한다. 기록은 이미 끝난 재판이다.
 */
@Getter
@Builder
public class GroupClosedTrialDto {

    /** 기소 ID. 판결 상세 화면으로 넘어갈 때 쓰는 키다. */
    private Long id;

    /* ── 소속 그룹 ────────────────────────────────────── */
    /** 그룹 기록에서는 화면이 이미 아는 값이지만, 전체 목록은 줄마다 그룹명을 보여 준다. */
    private Long groupId;

    private String groupName;

    /* ── 피고 ─────────────────────────────────────────── */
    private Long userId;
    private String nickname;
    /** 저장된 키가 아니라 조립된 URL 이다. 프로필을 안 올렸으면 NULL — 화면이 이니셜을 그린다. */
    private String profileImageUrl;

    /** {@code GUILTY} · {@code INNOCENT}. */
    private String status;

    /**
     * 어떻게 확정됐는지 — {@code VOTE} · {@code NO_VOTE} · {@code CONFESSION} · {@code AI_JUDGMENT}.
     *
     * <p><b>빼면 안 된다.</b> 프론트의 {@code verdictRouteName()} 이 이 값으로 이동할 화면을
     * 가른다. 없으면 AI 가 판결한 재판이 일반 판결문 화면으로 열린다.
     */
    private String verdictMethod;

    /** 판사 탕이의 판결 사유. {@code AI_JUDGMENT} 가 아니면 NULL 이다. */
    private String aiVerdictReason;

    /** 위반한 날짜. 「8월 5일 결산」의 그 날짜다. */
    private LocalDate settlementDate;

    /** 한도 초과액. */
    private BigDecimal exceededAmount;

    /* ── 로그인 사용자 기준 ───────────────────────────── */
    /** 내가 피고인지. JSON 키는 {@code mine} 이다(Lombok 이 {@code isMine()} 을 만든다). */
    private boolean mine;
    /** 피고가 변론을 냈는지. */
    private boolean defended;
    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 안 던졌거나 내가 피고면 NULL. */
    private String myVote;

    /* ── 개표 ─────────────────────────────────────────── */
    private int guiltyCount;
    private int innocentCount;
    /** 참여자 - 피고 1명. */
    private int totalVoters;

    private LocalDateTime createdAt;

    /** 판결이 확정된 시각. 목록이 이 값의 내림차순으로 온다. 근거는 Row 주석 참고. */
    private LocalDateTime confirmedAt;
}

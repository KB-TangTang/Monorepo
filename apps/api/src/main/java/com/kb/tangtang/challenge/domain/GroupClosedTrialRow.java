package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 확정된 재판 기록 한 줄의 원재료.
 *
 * <p>확정된 기소({@code GUILTY} · {@code INNOCENT})만 담는다. 진행 중인 기소는
 * {@link GroupIndictmentRow} 가 받는다.
 *
 * <p><b>{@link GroupIndictmentRow} 를 늘려 쓰지 않는다.</b> 그쪽은 진행 중 카드 두 화면
 * ({@code findOpenByGroupId} · {@code findOpenByUserId})이 select 조각을 공유하고 있어,
 * 필드를 더하면 한쪽 SQL 만 고쳤을 때 다른 화면에서 조용히 0·NULL 이 된다. 축이 다르기도 하다 —
 * 저쪽은 <b>마감</b>이 기준이고 여기는 <b>확정 시각·개표</b>가 기준이다.
 *
 * <p><b>투표 집계를 가리지 않는다.</b> 가리는 이유(이슈 #171 의 편승 투표 방지)는 투표가
 * 열려 있을 때만 성립한다. 확정된 재판은 개표가 끝난 재판이다.
 */
@Getter
@Setter
public class GroupClosedTrialRow {

    private Long id;

    /** 소속 그룹. 내 전체 기록 목록에서 줄마다 어느 그룹 것인지가 필요하다. */
    private Long groupId;

    private String groupName;

    /** 피고. */
    private Long userId;

    private String nickname;

    /** 원본 키다. URL 변환은 서비스가 {@code ImageStorage} 로 한다. */
    private String profileImageKey;

    /** {@code GUILTY} · {@code INNOCENT} 둘 중 하나. */
    private String status;

    /**
     * 어떻게 확정됐는지 ({@link VerdictMethod}).
     *
     * <p><b>반드시 내려보낸다.</b> 프론트가 이 값으로 AI 판결 화면과 일반 판결문 화면을 가른다.
     * 빠지면 AI 가 결정한 재판이 엉뚱한 화면으로 열린다.
     */
    private String verdictMethod;

    /** 판사 탕이의 사유. {@code AI_JUDGMENT} 가 아니면 NULL 이다({@code ck_ind_ai_reason}). */
    private String aiVerdictReason;

    /** 위반한 날짜. 기소 생성일이 아니다 — 심야 거래는 다음 날 배치가 잡아 하루 어긋난다. */
    private LocalDate challengeDate;

    /** 한도 초과액. */
    private BigDecimal exceededAmount;

    /** 피고가 변론을 냈는지. */
    private boolean defended;

    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 안 던졌거나 내가 피고면 NULL. */
    private String myVerdict;

    private int guiltyCount;

    private int innocentCount;

    /** 참여자 - 피고 1명. */
    private int totalVoters;

    private LocalDateTime createdAt;

    /**
     * 판결이 확정된 시각. 목록 정렬 기준이다.
     *
     * <p><b>{@code tbl_indictment.updated_at} 을 그대로 쓴다.</b> 확정 시각 전용 컬럼이 없어서다.
     * 스키마에 {@code ON UPDATE CURRENT_TIMESTAMP} 가 걸려 있지만, 상태 전이 UPDATE 4개
     * ({@code moveToVoting} · {@code confirmConfession} · {@code confirmVerdict} ·
     * {@code moveExpiredDefensesToVoting})가 <b>모두 WHERE 에서 GUILTY·INNOCENT 를 제외</b>하므로
     * 확정된 뒤로는 갱신되지 않는다.
     *
     * <p>⚠ 확정된 기소를 UPDATE 하는 구문이 새로 생기면 이 전제가 깨져 확정 시각이 밀린다.
     * 그때는 {@code confirmed_at} 컬럼을 추가하는 마이그레이션이 필요하다.
     */
    private LocalDateTime confirmedAt;
}

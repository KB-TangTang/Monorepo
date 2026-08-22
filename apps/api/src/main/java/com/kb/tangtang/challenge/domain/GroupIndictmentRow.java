package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 진행 중인 재판 카드 한 장의 원재료 (이슈 #169 · #432).
 *
 * <p>진행 중인 기소({@code DEFENSE_WAIT} · {@code VOTING})만 담는다. 확정된 기소는
 * 카드가 아니라 전적으로 간다.
 *
 * <p>그룹 상세({@code findOpenByGroupId})와 지방법원 홈({@code findOpenByUserId})이
 * <b>같은 select 조각을 공유한다.</b> 필드를 더할 때 한쪽 SQL 만 고치면 다른 화면에서
 * 조용히 0·NULL 이 된다.
 */
@Getter
@Setter
public class GroupIndictmentRow {

    private Long id;

    /**
     * 소속 그룹 (이슈 #432).
     *
     * <p>그룹 상세에서는 이미 아는 값이지만, 지방법원 홈은 여러 그룹의 재판을 한 목록에
     * 섞어 내려서 카드마다 어느 그룹 것인지가 필요하다.
     */
    private Long groupId;

    private String groupName;

    /** 피고. */
    private Long userId;

    private String nickname;

    /** 원본 키다. URL 변환은 서비스가 {@code ImageStorage} 로 한다. */
    private String profileImageKey;

    /** {@link IndictmentStatus} 의 이름. */
    private String status;

    /** 위반한 날짜. 기소 생성일이 아니다 — 심야 거래는 다음 날 배치가 잡아 하루 어긋난다. */
    private LocalDate challengeDate;

    /** 한도 초과액. */
    private BigDecimal exceededAmount;

    /** 피고가 변론을 냈는지. 카드가 「변론 완료」로 바뀌는 조건이다. */
    private boolean defended;

    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 아직 안 던졌으면 NULL. */
    private String myVerdict;

    private int voteCount;

    /** 참여자 - 피고 1명. */
    private int totalVoters;

    /** 마감 계산의 기준점. {@link TrialTodoRow#getCreatedAt()} 과 같은 사정이다. */
    private LocalDateTime createdAt;
}

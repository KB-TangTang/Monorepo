package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 그룹 상세 재판 카드 한 장의 원재료 (이슈 #169).
 *
 * <p>진행 중인 기소({@code DEFENSE_WAIT} · {@code VOTING})만 담는다. 확정된 기소는
 * 카드가 아니라 전적으로 간다.
 */
@Getter
@Setter
public class GroupIndictmentRow {

    private Long id;

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

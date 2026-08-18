package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 그룹 상세의 「오늘의 소비 상태」 카드 한 장의 원재료 (이슈 #169).
 *
 * <p>참여자 <b>전원</b>이 담긴다. 나를 빼는 일은 서비스가 한다 — 내 몫은 먹지 카드가 따로 쓰는데,
 * 여기서 빼 버리면 같은 값을 얻으려고 쿼리를 한 번 더 돌려야 한다.
 */
@Getter
@Setter
public class GroupMemberConsumptionRow {

    private Long userId;

    private String nickname;

    /** 원본 키다. URL 변환은 서비스가 {@code ImageStorage} 로 한다. */
    private String profileImageKey;

    /**
     * 평가 주기에 맞춘 소비액. 일일평가는 오늘 하루치, 기간평가는 기간 전체 합계다.
     *
     * <p>집계 행이 아직 없는 참여자도 0 으로 내려간다 — 카드가 빠지면 인원이 모자라 보인다.
     */
    private BigDecimal amount;
}

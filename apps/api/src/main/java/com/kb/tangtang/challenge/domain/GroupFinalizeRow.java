package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 최종 결과 확정 배치가 참여자 한 명을 판정하는 데 필요한 전부 (이슈 #172).
 *
 * <p>탈락 판정에 쓰는 값이 평가 주기마다 다르다 — 일일평가는 {@code livesCount}, 기간평가는
 * {@code chargeAmount} 다. 둘을 한 행에 같이 실어 두면 배치가 그룹당 조회 한 번으로 끝난다.
 */
@Getter
@Setter
public class GroupFinalizeRow {

    private Long userId;

    /** 남은 목숨. 일일평가에서 {@code 0} 이면 탈락이다. 기간평가는 항상 1로 시작해 쓰이지 않는다. */
    private Integer livesCount;

    /**
     * 챌린지 기간 전체의 실효 소비액. 무죄 판결로 인정된 감액이 이미 빠져 있다.
     *
     * <p>{@code final_charge_amount} 로 그대로 저장되고, 생존자 순위(오름차순)의 기준이 된다.
     * 집계 행이 하나도 없는 참여자도 0 으로 내려온다.
     */
    private BigDecimal chargeAmount;
}

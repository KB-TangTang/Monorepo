package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 결산 구간의 거래 목록 (이슈 #170).
 *
 * <p><b>피고 본인만</b> 받는 응답이다. 재판 상세({@link GroupTrialDetailDto})는 투표자도 보므로
 * 거래 목록을 거기 섞으면 남의 거래내역이 투표자에게 흘러간다 — 그래서 엔드포인트를 나눴다.
 *
 * <p><b>기소를 발화시킨 거래 1건이 아니라 구간 전체다.</b> 한도 초과는 누적으로 판정되므로
 * 발화 거래가 문제의 거래라는 보장이 없다. 친구들 몫까지 대납한 큰 거래는 그 시점에 한도
 * 미달이어서 기소를 만들지 않고, 뒤따르는 평범한 소비가 기소를 만든다 — 발화 거래만 보이면
 * 정작 대납한 금액을 신고할 방법이 없다.
 *
 * <p><b>{@code transactions} 의 합계는 {@code currentAmount} 와 맞아야 한다.</b> 화면이
 * 「실제 부담금 합계 vs 한도」를 계산하는 기준이라 어긋나면 판정이 무너진다. 그래서 조회 조건을
 * 집계 배치와 같은 {@code consumptionFilter} 조각으로 공유한다
 * ({@code GroupChallengeResultMapper.xml}).
 *
 * <p>단 <b>무죄 감액이 있으면 두 값이 다르다</b> — {@code currentAmount} 는 감액을 뺀 값이고
 * 거래 목록은 원본이다. 감액은 #172 가 기록하므로 이 시점에는 항상 0 이다.
 */
@Getter
@Builder
public class TrialTransactionsDto {

    /** {@code DAILY} · {@code PERIOD}. 화면이 「일일결산 / 기간결산」 문구를 이 값으로 고른다. */
    private String evalType;

    private BigDecimal limitAmount;

    /**
     * 결산 구간의 소비액(무죄 감액 반영). 재판 상세의 {@code currentAmount} 와 같은 값이다 —
     * 두 화면이 다른 숫자를 보이면 안 된다.
     */
    private BigDecimal currentAmount;

    /** 거래가 있는 날만. 날짜 오름차순. */
    private List<TrialTransactionDayDto> days;
}

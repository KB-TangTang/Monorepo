package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 변론 화면의 결산 구간 거래 한 건 (이슈 #170).
 *
 * <p><b>기소를 발화시킨 거래 1건이 아니다.</b> 한도 초과는 누적으로 판정되므로 발화 거래가 문제의
 * 거래라는 보장이 없다 — 친구들 몫까지 대납한 큰 거래는 그 시점에 한도 미달이어서 기소를 만들지
 * 않고, 뒤따르는 평범한 소비가 기소를 만든다. 그래서 <b>구간 전체</b>를 내려보내 건별로
 * 실제 부담금을 받는다.
 *
 * <p>집계와 목록이 반드시 같은 거래를 세야 해서 조건을
 * {@code GroupChallengeResultMapper.xml} 의 {@code consumptionFilter} 조각으로 공유한다.
 */
@Getter
@Setter
public class TrialTransactionRow {

    private Long transactionId;

    private LocalDate trDate;

    /** {@code tbl_transaction.tr_time}. 원천에 시각이 없으면 NULL 이다. */
    private LocalTime trTime;

    private String merchantName;

    /**
     * 부호 있는 금액. 환불은 음수다 — 집계({@code upsertDailyResults})와 같은 규칙이라
     * 화면에서 목록을 더하면 그 날 {@code dailyAmount} 가 나온다.
     */
    private BigDecimal amount;

    /** LLM 분류 전이면 NULL. */
    private String categoryName;

    private boolean isRefund;

    /* ── 결제수단 라벨 원재료. 조립은 PaymentMethodLabels 가 한다 ── */
    private String sourceType;

    private String direction;

    private String cardInstitutionName;

    private String accountBankName;
}

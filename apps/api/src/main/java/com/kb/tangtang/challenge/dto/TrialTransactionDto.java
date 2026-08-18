package com.kb.tangtang.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 변론 화면의 거래 한 건 (이슈 #170).
 *
 * <p>화면이 이 목록을 돌며 건별로 「실제 내가 쓴 금액」을 입력받는다
 * ({@code DefenseActualCostView.vue}). 입력값은 서버로 올라오지 않고 <b>합계만</b> 전송된다.
 *
 * <p><b>날짜 필드가 없다.</b> 날짜는 {@link TrialTransactionDayDto#getDate()} 가 갖는다 —
 * 같은 값을 건마다 반복하면 기간결산에서 응답이 쓸데없이 커진다. 시각만 따로 두는 이유는
 * {@code tr_time} 이 NULL 일 수 있어서다({@code date + time} 을 하나로 합치면 NULL 시각에
 * 없는 시간을 지어내야 한다).
 */
@Getter
@Builder
public class TrialTransactionDto {

    private Long transactionId;

    /** {@code tbl_transaction.tr_time}. 원천에 시각이 없으면 NULL — 화면이 시각을 생략한다. */
    private LocalTime time;

    private String merchantName;

    /** 환불은 음수다. 하루 목록을 더하면 그 날 {@code dailyAmount} 가 나온다. */
    private BigDecimal amount;

    /** LLM 분류 전이면 NULL. */
    private String categoryName;

    /** 「KB국민카드」 처럼 조립된 라벨. 규칙은 {@code common.util.PaymentMethodLabels} 가 소유한다. */
    private String paymentMethod;

    /*
     * ⚠ boolean isRefund 는 Lombok 이 isRefund() 를 만들고 Jackson 은 그 getter 를 "refund" 로
     *   읽는다. 프론트는 isRefund 를 본다 → 이름을 고정하지 않으면 항상 undefined 다.
     *   (transaction/dto/TransactionListItemDto.java:25-31 과 같은 함정)
     */
    @Getter(onMethod_ = @JsonProperty("isRefund"))
    private boolean isRefund;
}

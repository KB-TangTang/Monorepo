package com.kb.tangtang.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 장부 화면 한 행. amount 는 부호 있는 값이다 — CONSUMPTION 은 음수, INCOME 은 양수,
 * 환불(is_refund=1)은 지출을 상계하는 양수, TRANSFER 는 direction 기준(IN 양수/OUT 음수).
 */
@Getter
@Builder
public class TransactionListItemDto {

    private Long id;
    private String date;
    private String merchant;
    private String category;
    private String paymentMethod;
    private String classification;
    private BigDecimal amount;

    /*
     * ⚠ boolean isRefund 는 Lombok 이 isRefund() 를 만들고, Jackson 은 그 getter 를 "refund" 로
     *   읽는다(notification/dto/NotificationDto.java 의 isRead 와 같은 함정). 프론트는
     *   transaction.isRefund 를 본다 → 이름을 고정하지 않으면 항상 undefined 다.
     */
    @Getter(onMethod_ = @JsonProperty("isRefund"))
    private boolean isRefund;
}

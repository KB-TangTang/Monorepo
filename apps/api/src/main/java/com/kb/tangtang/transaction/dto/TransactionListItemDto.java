package com.kb.tangtang.transaction.dto;

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
}

package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class FixedExpensePaymentHistoryDto {

    private long id;
    private LocalDate date;
    private String provider;
    private BigDecimal amount;
}

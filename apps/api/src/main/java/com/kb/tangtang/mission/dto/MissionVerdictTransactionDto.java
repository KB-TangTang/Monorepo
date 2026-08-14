package com.kb.tangtang.mission.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MissionVerdictTransactionDto {

    private Long transactionId;
    private String merchantName;
    private BigDecimal amount;
}

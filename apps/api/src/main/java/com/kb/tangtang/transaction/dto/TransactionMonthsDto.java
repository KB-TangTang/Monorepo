package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TransactionMonthsDto {

    private List<TransactionMonthDto> months;
}

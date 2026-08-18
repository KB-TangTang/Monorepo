package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** yearMonth 생략 시 period=null·summary=null 이고 transactions 는 데이터 있는 전체 월을 합친 값이다. */
@Getter
@Builder
public class TransactionListResponseDto {

    private String period;
    private LedgerSummaryDto summary;
    private List<TransactionListItemDto> transactions;
}

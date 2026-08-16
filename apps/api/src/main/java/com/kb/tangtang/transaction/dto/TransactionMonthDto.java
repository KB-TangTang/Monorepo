package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

/** 장부 월 이동 네비게이션 한 칸. hasData=false 는 그 달에 아직 거래가 없다는 뜻. */
@Getter
@Builder
public class TransactionMonthDto {

    private String value;
    private boolean hasData;
}

package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관별로 묶은 계좌 목록. 화면이 기관 헤더 아래 계좌를 나열한다.
 */
@Getter
@Builder
public class LinkableGroupDto {

    private final String bankCode;
    private final String bankName;
    private final String shortLabel;
    private final java.util.List<LinkableAccountDto> accounts;
}

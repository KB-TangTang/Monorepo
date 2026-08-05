package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 지원 금융기관 한 곳 (AC_01_01).
 * connected=true 면 이미 연결돼 있어 다시 고를 수 없다.
 */
@Getter
@Builder
public class InstitutionDto {

    private final String code;
    private final String name;
    private final String shortLabel;
    private final boolean connected;
}

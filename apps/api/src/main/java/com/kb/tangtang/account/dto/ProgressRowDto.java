package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 조회 진행 화면의 기관 한 줄.
 */
@Getter
@Builder
public class ProgressRowDto {

    private final String code;
    private final String name;
    private final String status;
}

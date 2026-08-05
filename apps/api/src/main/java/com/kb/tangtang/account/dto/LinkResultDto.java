package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 저장 결과.
 */
@Getter
@Builder
public class LinkResultDto {

    private final int linkedCount;
}

package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 승인 여부 폴링 응답.
 */
@Getter
@Builder
public class AuthStatusDto {

    private final String status;
}

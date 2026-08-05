package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 간편인증 수단 (카카오톡 · PASS · 네이버).
 */
@Getter
@Builder
public class ProviderDto {

    private final String code;
    private final String name;
}

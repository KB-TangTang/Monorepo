package com.kb.tangtang.account.client.stock;

import lombok.Builder;
import lombok.Getter;

/** 토스증권 OAuth2 토큰 발급 응답 한 장. */
@Getter
@Builder
public class TossAccessToken {
    private final String accessToken;
    private final long expiresInSeconds;
}

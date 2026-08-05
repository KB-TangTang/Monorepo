package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결(인증) 요청 결과. 간편인증과 기관 로그인이 같은 모양을 돌려준다 —
 * 이후 단계는 어느 경로로 왔는지 모른다.
 */
@Getter
@Builder
public class ConnectionResponseDto {

    private final String connectionId;
    private final String status;
    private final int expiresInSeconds;
    private final boolean needsExtraAuth;
    private final String extraAuthType;
}

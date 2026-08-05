package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 지원 인증 수단 한 종류.
 * type 이 SIMPLE_AUTH 면 providers 가 채워지고, INSTITUTION_LOGIN 이면 비어 있다.
 */
@Getter
@Builder
public class AuthMethodDto {

    private final String type;
    private final java.util.List<ProviderDto> providers;

    /**
     * 본인 정보(이름·생년월일·통신사·휴대폰)를 함께 보내야 하는지.
     * 프론트는 이 값이 true 일 때만 개인정보를 전송한다 — 목 모드에서는 화면 밖으로 나가지 않는다.
     */
    private final boolean requiresIdentity;
}

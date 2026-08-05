package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기관 로그인 인증 요청 본문. 기관 수만큼 자격증명이 들어온다.
 */
@Getter
@Setter
public class InstitutionLoginRequestDto {

    private java.util.List<CredentialRequestDto> credentials;
}

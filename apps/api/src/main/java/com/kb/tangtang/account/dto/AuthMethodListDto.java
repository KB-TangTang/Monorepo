package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * **분기의 유일한 근거.** 프론트는 이 응답만 보고 인증 화면을 고르며
 * 자기가 목 모드인지 CODEF 모드인지 알지 못한다. (DECISIONS.md 2026-08-05)
 */
@Getter
@Builder
public class AuthMethodListDto {

    private final java.util.List<AuthMethodDto> methods;
}

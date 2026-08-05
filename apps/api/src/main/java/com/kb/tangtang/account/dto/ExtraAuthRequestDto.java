package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 2-way 추가인증 제출 (SMS 인증번호 등).
 */
@Getter
@Setter
public class ExtraAuthRequestDto {

    private String authCode;
}

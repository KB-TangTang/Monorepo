package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기관 로그인 자격증명 한 건.
 *
 * ⚠ 로그에 찍히면 안 되는 값이다. Lombok @ToString 을 붙이지 않은 이유다.
 *   서비스는 이 값을 클라이언트로 넘기기만 하고 저장하지 않는다.
 */
@Getter
@Setter
public class CredentialRequestDto {

    private String organization;

    private String loginType;

    private String id;

    private String password;
}

package com.kb.tangtang.account.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관 로그인 자격증명. INSTITUTION_LOGIN 경로에서만 쓴다.
 *
 * ⚠ 이 객체는 요청 처리 중에만 살아 있어야 한다.
 *   DB·로그·캐시 어디에도 남기지 않는다. toString 을 만들지 않은 것도 그 때문이다
 *   (Lombok @Data 를 쓰면 비밀번호가 로그에 찍힌다).
 */
@Getter
@Builder
public class CredentialDto {

    /** 기관 코드 (CODEF organization) */
    private final String organization;

    /** 로그인 방식. CODEF 는 "1" = 아이디/비밀번호 */
    private final String loginType;

    private final String id;

    private final String password;
}

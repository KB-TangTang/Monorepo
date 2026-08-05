package com.kb.tangtang.account.client.dto;

import com.kb.tangtang.account.domain.AuthMethod;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 연결(인증) 요청. 두 인증 수단이 같은 타입을 쓴다.
 *
 * SIMPLE_AUTH        → provider + organizations
 * INSTITUTION_LOGIN  → credentials (기관 수만큼)
 *
 * ⚠ 개인정보(identity)는 **실 CODEF 간편인증에서만** 채워진다.
 *   목 모드에는 전달할 인증기관이 없어 화면에서 형식만 검증하고 폐기한다.
 *   어느 쪽인지는 auth-methods 응답의 requiresIdentity 가 알려준다. (설계서 §5.5)
 */
@Getter
@Builder
public class ConnectionRequest {

    private final AuthMethod authMethod;

    /** 간편인증 수단 코드. 실 CODEF loginTypeLevel 과 1:1 대응한다 (KAKAO=1 · PASS=5 · NAVER=6) */
    private final String provider;

    /** 연결 대상 기관 코드 목록 */
    private final List<String> organizations;

    /** 기관 로그인 자격증명. SIMPLE_AUTH 에서는 비어 있다 */
    private final List<CredentialDto> credentials;

    /**
     * 간편인증 본인 정보. 목 모드에서는 null 이다.
     * 실 CODEF 간편인증에서만 필요하며, 서버는 전달만 하고 저장하지 않는다.
     */
    private final IdentityDto identity;
}

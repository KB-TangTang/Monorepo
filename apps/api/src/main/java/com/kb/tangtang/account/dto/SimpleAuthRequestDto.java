package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 간편인증 요청 본문.
 *
 * ⚠ **개인정보는 필요할 때만 온다.** 목 모드에는 전달할 인증기관이 없어
 *   화면에서 형식만 검증하고 폐기하며, 이 필드들은 비어 있다.
 *   실 CODEF 간편인증에서만 채워진다. (설계서 §5.5)
 */
@Getter
@Setter
public class SimpleAuthRequestDto {

    private String provider;

    private java.util.List<String> organizations;

    /*
     * 본인 정보. auth-methods 의 requiresIdentity 가 true 일 때만 채워져 온다.
     * ⚠ 서버는 CODEF 로 전달만 하고 저장하지 않는다. 로그로도 남기지 않는다.
     */
    private String userName;

    private String birthDate;

    private String carrier;

    private String phoneNo;
}

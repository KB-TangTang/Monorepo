package com.kb.tangtang.account.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 간편인증에 필요한 본인 정보.
 *
 * 실제 마이데이터에서 이 값들은 **"누구에게 인증 푸시를 보낼지"** 를 정한다.
 * 목 모드에는 보낼 인증기관이 없어 화면에서 형식만 검증하고 폐기하지만,
 * 실 CODEF 간편인증에서는 요청에 실어야 한다.
 *
 * ⚠ 서버는 이 값을 **CODEF 로 전달만 하고 저장하지 않는다.** 로그로도 남기지 않는다
 *   (그래서 toString 을 만들지 않았다).
 * ⚠ 어떤 화면에서 이 값을 보낼지는 프론트가 정하지 않는다 —
 *   `auth-methods` 응답의 `requiresIdentity` 가 정한다.
 */
@Getter
@Builder
public class IdentityDto {

    /** 이름 */
    private final String userName;

    /** 생년월일. 화면은 6자리로 받고 서버가 8자리(yyyyMMdd)로 바꾼다 */
    private final String birthDate;

    /** 통신사 (SKT · KT · LG U+ · 알뜰폰) */
    private final String carrier;

    /** 휴대폰번호. 하이픈 없이 숫자만 */
    private final String phoneNo;
}

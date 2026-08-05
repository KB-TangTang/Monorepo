package com.kb.tangtang.account.client.dto;

import com.kb.tangtang.account.domain.AuthStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 연결 요청 결과.
 *
 * 실 CODEF 의 POST /v1/account/create 는 successList / errorList 를 **기관 단위로** 돌려준다
 * (2026-08-05 실계좌 확인). 그 구조를 그대로 담아 진행 화면의 기관별 상태로 쓴다.
 */
@Getter
@Builder
public class ConnectionResult {

    /** CODEF connectedId 또는 목 모드의 대체 식별자 */
    private final String connectionId;

    private final AuthStatus status;

    /** 간편인증 승인 제한 시간(초). 기관 로그인 경로에서는 0 */
    private final int expiresInSeconds;

    /** 실 CODEF 가 기관에 따라 요구하는 2-way 추가인증 */
    private final boolean needsExtraAuth;

    private final String extraAuthType;

    /** 인증에 성공한 기관 코드 */
    private final List<String> successOrganizations;

    /** 인증에 실패한 기관 코드 */
    private final List<String> failedOrganizations;
}

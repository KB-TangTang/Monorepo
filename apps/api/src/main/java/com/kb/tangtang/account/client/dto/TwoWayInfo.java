package com.kb.tangtang.account.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * CODEF 간편인증 2-way 세션 정보.
 *
 * 1차 요청이 `CF-03002`(추가 인증 필요)를 돌려줄 때 함께 오는 값들이다.
 * 사용자가 인증 앱에서 승인한 뒤 **같은 값을 그대로 실어 2차 요청**을 보내야 연결이 성립한다.
 * (출처: CODEF 개발가이드 「간편인증 재요청」)
 *
 * ⚠ 이 값은 세션 식별자이지 개인정보가 아니다. 다만 유효시간이 있어 오래 들고 있으면 만료된다.
 */
@Getter
@Builder
public class TwoWayInfo {

    private final Long jobIndex;

    private final Long threadIndex;

    private final String jti;

    private final Long twoWayTimestamp;
}

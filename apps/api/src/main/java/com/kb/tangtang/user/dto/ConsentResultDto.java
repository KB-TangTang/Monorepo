package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 저장·철회 응답.
 * 프론트가 저장 직후 auth 스토어의 게이트 플래그를 갱신해야 하므로
 * 별도 조회를 한 번 더 하지 않도록 여기 실어 보낸다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentResultDto {

    /** SIGNUP 필수 동의 미완료. */
    private boolean needsConsent;

    /**
     * `THIRD_PARTY`(제3자 제공) 동의 미완료. 계좌 연동 직전 단계다.
     * 저장 직후 프론트가 온보딩 게이트를 갱신할 수 있게 함께 내려준다.
     * (DECISIONS.md 2026-08-11 (7))
     */
    private boolean needsFinancialConsent;
}

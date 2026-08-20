package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 저장 결과.
 */
@Getter
@Builder
public class LinkResultDto {

    private final int linkedCount;

    /**
     * 은행 계좌는 0개(linkedCount=0)여도, 선택한 기관 중 대출·페이머니가 있으면 true(#334).
     * 그 업권은 여기서 저장되지 않고 완료 화면의 최초 동기화가 저장한다 — 그래도 사용자 입장에서는
     * "연동을 마쳤다"가 맞으므로, 프론트 라우터 가드(canEnterLinkStep)가 이 값도 함께 본다.
     */
    @Builder.Default
    private final boolean directAssetsPending = false;
}

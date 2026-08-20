package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관별로 묶은 계좌 목록. 화면이 기관 헤더 아래 계좌를 나열한다.
 */
@Getter
@Builder
public class LinkableGroupDto {

    private final String bankCode;
    private final String bankName;
    private final String shortLabel;
    private final java.util.List<LinkableAccountDto> accounts;

    /**
     * 대출·페이머니 미리보기 그룹인지(#334). 이 그룹은 사용자가 고를 수 없다 — 은행처럼 여러 계좌 중
     * 일부만 고르는 개념이 없고(기관당 정확히 1건), 실제 연결도 여기(link())가 아니라 계좌 연동 완료
     * 직후의 최초 동기화가 만든다. 화면은 이 값이 true면 체크박스 없이 "자동으로 연동돼요"로만 보여준다.
     */
    @Builder.Default
    private final boolean autoIncluded = false;
}

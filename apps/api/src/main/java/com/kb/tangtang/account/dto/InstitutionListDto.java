package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관 목록. 프론트가 은행·카드·증권·대출·페이머니 순으로 섹션을 그린다.
 *
 * 보험은 연동 범위에서 제외됐다(2026-08-06). 되살리려면 InstitutionCatalog 부터 손봐야 한다.
 *
 * 대출·페이머니는 2026-08-19 에 추가했다(#344). JSON 키가 `loans`·`payMoney` 그대로여야
 * 프론트가 섹션을 찾는다 — 필드명을 바꾸면 화면에서 그 업권이 사라진다.
 */
@Getter
@Builder
public class InstitutionListDto {

    private final java.util.List<InstitutionDto> banks;
    private final java.util.List<InstitutionDto> cards;
    private final java.util.List<InstitutionDto> securities;
    private final java.util.List<InstitutionDto> loans;
    private final java.util.List<InstitutionDto> payMoney;
}

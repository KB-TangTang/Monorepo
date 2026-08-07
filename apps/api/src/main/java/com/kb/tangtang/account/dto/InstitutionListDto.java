package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관 목록. 프론트가 은행·카드·증권 순으로 섹션을 그린다.
 *
 * 보험은 연동 범위에서 제외됐다(2026-08-06). 되살리려면 InstitutionCatalog 부터 손봐야 한다.
 */
@Getter
@Builder
public class InstitutionListDto {

    private final java.util.List<InstitutionDto> banks;
    private final java.util.List<InstitutionDto> cards;
    private final java.util.List<InstitutionDto> securities;
}

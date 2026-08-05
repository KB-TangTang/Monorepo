package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관 목록. 프론트가 은행·카드·증권·보험 순으로 섹션을 그린다.
 */
@Getter
@Builder
public class InstitutionListDto {

    private final java.util.List<InstitutionDto> banks;
    private final java.util.List<InstitutionDto> cards;
    private final java.util.List<InstitutionDto> securities;
    private final java.util.List<InstitutionDto> insurances;
}

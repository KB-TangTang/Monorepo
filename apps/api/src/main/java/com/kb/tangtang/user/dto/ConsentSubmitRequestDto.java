package com.kb.tangtang.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** POST /api/consents 요청 본문. */
@Getter
@Setter
@NoArgsConstructor
public class ConsentSubmitRequestDto {

    private String scope;
    private List<ConsentAgreementDto> agreements;
}

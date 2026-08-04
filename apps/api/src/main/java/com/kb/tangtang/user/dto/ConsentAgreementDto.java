package com.kb.tangtang.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 저장 요청 안의 항목 1건. */
@Getter
@Setter
@NoArgsConstructor
public class ConsentAgreementDto {

    private String type;
    private boolean agreed;
}

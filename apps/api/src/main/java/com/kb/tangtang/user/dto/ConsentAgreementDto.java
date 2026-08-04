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

    /**
     * agreed 를 생략하거나 null 로 보내면 Jackson 이 원시형 기본값 false 로 바인딩한다.
     * 그 결과는 해당 항목을 요청에 아예 넣지 않은 것과 동일하다 — 둘 다 미동의로 저장된다.
     * 의도된 동작이다: 필수 항목은 저장 전에 CONSENT_REQUIRED_MISSING 으로 걸러지고,
     * 선택 항목은 옵트인 원칙상 "명시적 동의가 없으면 미동의"가 안전한 기본값이다.
     * 따라서 동의를 기록하려면 agreed: true 를 명시적으로 보내야 한다.
     */
    private boolean agreed;
}

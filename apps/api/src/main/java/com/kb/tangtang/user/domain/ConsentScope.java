package com.kb.tangtang.user.domain;

import java.util.List;

/**
 * 동의 그룹. 어떤 화면에서 어떤 항목을 한 번에 받는지를 정의한다.
 *
 * needsConsent 판정은 SIGNUP 필수 항목만 본다.
 * THIRD_PARTY 를 포함하면 계좌를 아직 연동하지 않은 사용자가 동의 화면을 벗어나지 못한다.
 */
public enum ConsentScope {

    SIGNUP(List.of(
            ConsentType.TERMS,
            ConsentType.PRIVACY,
            ConsentType.FINANCIAL_DATA,
            ConsentType.AI_USAGE,
            ConsentType.MARKETING)),

    FINANCIAL(List.of(ConsentType.THIRD_PARTY)),

    CHALLENGE(List.of(ConsentType.CHALLENGE));

    private final List<ConsentType> types;

    ConsentScope(List<ConsentType> types) {
        this.types = types;
    }

    /** 이 그룹의 전체 항목. 저장 시 "미동의 간주" 범위이기도 하다. */
    public List<ConsentType> types() {
        return types;
    }

    public List<ConsentType> requiredTypes() {
        return types.stream().filter(ConsentType::required).toList();
    }
}

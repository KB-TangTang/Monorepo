package com.kb.tangtang.user.domain;

/**
 * 동의 항목. tbl_user_consent.consent_type 과 이름이 1:1로 대응한다.
 *
 * required     — 필수 동의 여부. tbl_user_consent.is_required 에 이 값을 쓴다(요청값 아님)
 * withdrawable — 철회 가능 여부. TERMS·PRIVACY 철회는 탈퇴에 해당하므로 막는다
 */
public enum ConsentType {

    TERMS("서비스 이용약관", true, false),
    PRIVACY("개인정보 수집·이용", true, false),
    FINANCIAL_DATA("금융정보 수집·이용", true, true),
    THIRD_PARTY("CODEF 제3자 제공", true, true),
    AI_USAGE("AI 소비 분석 활용", false, true),
    MARKETING("마케팅 알림 수신", false, true),
    /** 그룹챌린지 참가 동의. 아직 어떤 scope 에도 속하지 않는다(해당 이슈에서 scope 추가). */
    CHALLENGE("그룹챌린지 참가", false, true);

    private final String label;
    private final boolean required;
    private final boolean withdrawable;

    ConsentType(String label, boolean required, boolean withdrawable) {
        this.label = label;
        this.required = required;
        this.withdrawable = withdrawable;
    }

    public String label() { return label; }
    public boolean required() { return required; }
    public boolean withdrawable() { return withdrawable; }
}

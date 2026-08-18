package com.kb.tangtang.common.util;

/**
 * 거래 한 건의 결제수단 표시 라벨.
 *
 * <p>원래 {@code TransactionQueryService} 안의 private 메서드였다. 그룹챌린지 변론 화면(이슈 #170)이
 * 결산 구간의 거래 목록을 같은 형태로 보여줘야 해서 {@code common} 으로 끌어올렸다.
 * <b>복사하지 않는다</b> — 장부와 변론 화면이 같은 거래를 다른 이름으로 부르면 사용자가 자기가
 * 무엇으로 결제했는지 화면마다 다르게 읽게 된다.
 *
 * <p>{@code tbl_transaction} 에 결제수단 컬럼이 없어 {@code source_type} + 발급사·은행명으로 만든다.
 * 신용카드는 "{카드사명}카드", 체크카드는 "{카드사명} 체크카드"다
 * (2026-08-15 결정 — 정보량을 늘리는 쪽을 택했다).
 */
public final class PaymentMethodLabels {

    private PaymentMethodLabels() {
    }

    /**
     * @param sourceType          {@code tbl_transaction.source_type}. NULL 이면 "기타"
     * @param direction           {@code IN} · {@code OUT}. 은행 거래의 입금/출금을 가른다
     * @param cardInstitutionName {@code tbl_card.institution_name}. 카드가 아니면 NULL
     * @param accountBankName     {@code tbl_connected_account.bank_name}. 계좌가 아니면 NULL
     */
    public static String resolve(String sourceType, String direction,
                                 String cardInstitutionName, String accountBankName) {
        if (sourceType == null) {
            return "기타";
        }
        switch (sourceType) {
            case "CARD_CREDIT":
                return cardInstitutionName != null ? cardInstitutionName + "카드" : "신용카드";
            case "CARD_CHECK":
                return cardInstitutionName != null ? cardInstitutionName + " 체크카드" : "체크카드";
            case "BANK":
                if ("IN".equals(direction)) {
                    return "입금";
                }
                return accountBankName != null ? accountBankName + " 출금" : "출금";
            case "DEPOSIT":
                return accountBankName != null ? accountBankName + " 예금" : "예금";
            case "PAYMONEY":
                return accountBankName != null ? accountBankName : "페이머니";
            case "SECURITIES":
                return "증권";
            case "LOAN":
                return "대출";
            default:
                return sourceType;
        }
    }
}

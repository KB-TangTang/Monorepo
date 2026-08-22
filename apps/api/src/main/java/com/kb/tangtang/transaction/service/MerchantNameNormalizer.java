package com.kb.tangtang.transaction.service;

import java.util.regex.Pattern;

/**
 * 가맹점명·업종명·키워드 정규화. tbl_transaction.merchant_name_normalized 컬럼은 현재
 * FinancialSyncServiceImpl 이 원본을 그대로 복사만 해서 실질적으로 정규화돼 있지 않다
 * (이슈 #147 계획 문서 참고) — 이 클래스가 카테고리화 파이프라인의 유일한 정규화 규칙이다.
 */
public final class MerchantNameNormalizer {

    private static final Pattern CORPORATE_MARKERS =
            Pattern.compile("(주식회사|\\(주\\)|㈜|Co\\.,?\\s*Ltd\\.?|Corp\\.?|Inc\\.?)",
                    Pattern.CASE_INSENSITIVE);
    /** 한글·영문·숫자만 남긴다. 공백·특수문자는 전부 제거한다. */
    private static final Pattern NON_ALNUM_KOREAN = Pattern.compile("[^0-9A-Za-z가-힣]");

    private MerchantNameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = CORPORATE_MARKERS.matcher(raw).replaceAll("");
        value = NON_ALNUM_KOREAN.matcher(value).replaceAll("");
        return value.toLowerCase();
    }
}

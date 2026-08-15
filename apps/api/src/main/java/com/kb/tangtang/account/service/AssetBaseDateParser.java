package com.kb.tangtang.account.service;

import com.kb.tangtang.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** {@code baseDate} 쿼리 파라미터 파싱. AssetSummaryService·AssetTrendService 가 공통으로 쓴다. */
final class AssetBaseDateParser {

    private AssetBaseDateParser() {
    }

    static LocalDate parse(String rawBaseDate, LocalDate defaultDate) {
        if (rawBaseDate == null || rawBaseDate.isBlank()) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(rawBaseDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "baseDate는 YYYY-MM-DD 형식이어야 합니다.");
        }
    }
}

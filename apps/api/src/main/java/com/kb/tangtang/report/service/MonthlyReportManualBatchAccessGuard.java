package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 배포 환경에서도 쓰는 월간 리포트 수동 배치의 운영 키를 검증한다. */
@Component
public class MonthlyReportManualBatchAccessGuard {

    private final String configuredKey;

    public MonthlyReportManualBatchAccessGuard(
            @Value("${report.monthly.manual-batch.key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public void ensureAuthorized(String suppliedKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new BusinessException("MANUAL_REPORT_BATCH_DISABLED",
                    "월간 리포트 수동 배치 운영 키가 설정되지 않았습니다.");
        }
        if (suppliedKey == null || !MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("MANUAL_REPORT_BATCH_FORBIDDEN",
                    "월간 리포트 수동 배치 운영 키가 올바르지 않습니다.");
        }
    }
}

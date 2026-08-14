package com.kb.tangtang.account.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialSyncBatchPropertiesTest {

    @Test
    @DisplayName("금융 동기화 배치의 필수 기본 설정을 제공한다")
    void providesRequiredBatchProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            properties.load(input);
        }

        assertEquals("true", properties.getProperty("financial.sync.batch.enabled"));
        assertEquals("1800000", properties.getProperty("financial.sync.batch.fixed-delay-ms"));
        assertEquals("20", properties.getProperty("financial.sync.batch.max-users-per-tick"));
        assertEquals("25", properties.getProperty("financial.sync.batch.min-interval-minutes"));
    }
}

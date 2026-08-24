package com.kb.tangtang.report.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyReportBatchMapperXmlTest {

    private static final String RESOURCE = "mapper/report/MonthlyReportBatchMapper.xml";

    @Test
    void parsesRetryablePreviousMonthCandidateQuery() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = MonthlyReportBatchMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findEligibleCandidates"));
        assertTrue(configuration.hasStatement(namespace + ".findForceBatchCandidates"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("u.status = 'ACTIVE'"));
        assertTrue(xml.contains("u.created_at &lt; #{targetMonthEnd}"));
        assertTrue(xml.contains("s.`year_month` = #{yearMonth}"));
        assertTrue(xml.contains("LEFT JOIN tbl_user_consent c"));
        assertTrue(xml.contains("c.consent_type = 'AI_USAGE'"));
        assertTrue(xml.contains("AS ai_usage_consented"));
        assertTrue(xml.contains("s.ai_analysis_attempt_count &lt; #{maxAutoAttempts}"));
        assertTrue(xml.contains("s.ai_analysis_failed_at &lt;= #{retryNotBefore}"));
        assertTrue(xml.contains("'TOO_MANY_REQUESTS', 'AI_PROVIDER_UNAVAILABLE'"));
        assertTrue(xml.contains("s.category_summary_json"));
        assertTrue(xml.contains("s.ai_analysis_status"));
    }
}

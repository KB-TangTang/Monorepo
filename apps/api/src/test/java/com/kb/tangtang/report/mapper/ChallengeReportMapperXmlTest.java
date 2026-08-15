package com.kb.tangtang.report.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeReportMapperXmlTest {

    private static final String RESOURCE = "mapper/report/ChallengeReportMapper.xml";

    @Test
    void parsesMapperXmlAndRegistersStatements() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = ChallengeReportMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".hasActiveChallengeConsent"));
        assertTrue(configuration.hasStatement(namespace + ".findConfirmedReportMonths"));
        assertTrue(configuration.hasStatement(namespace + ".findFirstMissionMonth"));
        assertTrue(configuration.hasStatement(namespace + ".findChallengeConsentMonth"));

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            String xml = new String(inputStream.readAllBytes());
            assertFalse(xml.contains("${"));
            assertTrue(xml.contains("consent_type = 'CHALLENGE'"));
            assertTrue(xml.contains("tbl_challenge_monthly_report"));
            assertTrue(xml.contains("`year_month` &lt; #{currentYearMonth}"));
            assertTrue(xml.contains("ORDER BY `year_month` DESC"));
            assertTrue(xml.contains("tbl_user_mission_info"));
            assertTrue(xml.contains("assign_date &lt; #{currentMonthStart}"));
            assertTrue(xml.contains("created_at &lt; #{currentMonthStart}"));
        }
    }
}

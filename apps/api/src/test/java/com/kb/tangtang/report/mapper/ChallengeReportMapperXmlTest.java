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
        assertTrue(configuration.hasStatement(namespace + ".findFinalizedReportUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findFinalizedMissionRows"));
        assertTrue(configuration.hasStatement(namespace + ".findFirstMissionDate"));
        assertTrue(configuration.hasStatement(namespace + ".findDifficultyPolicies"));
        assertTrue(configuration.hasStatement(namespace + ".upsertMonthlyReport"));
        assertTrue(configuration.hasStatement(namespace + ".findMonthlyReport"));
        assertTrue(configuration.hasStatement(namespace + ".findPreviousMonthlyReport"));

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
            assertTrue(xml.contains("assignment.result IN ('SUCCESS', 'FAIL')"));
            assertTrue(xml.contains("assignment.base_amount IS NOT NULL"));
            assertTrue(xml.contains("<if test=\"!includeExisting\">"));
            assertTrue(xml.contains("mission_transaction.classification = 'CONSUMPTION'"));
            assertTrue(xml.contains("mission_transaction.is_excluded_from_summary = 0"));
            assertTrue(xml.contains("saved_amount"));
            assertTrue(xml.contains("over_amount"));
            assertTrue(xml.contains("category_results_json"));
            assertTrue(xml.contains("over_amount AS overspent_amount"));
            assertTrue(xml.contains("category_results_json AS category_effects_json"));
            assertTrue(xml.contains("SELECT MIN(assign_date)"));
            assertTrue(xml.contains("mission.mission_type"));
            assertFalse(xml.contains("mission_type = 'RELATIVE'"));
            assertTrue(xml.contains("CAST(#{weeklyResultsJson} AS JSON)"));
            assertTrue(xml.contains("ON DUPLICATE KEY UPDATE"));
        }
    }
}

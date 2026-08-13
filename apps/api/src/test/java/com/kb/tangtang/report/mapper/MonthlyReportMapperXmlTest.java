package com.kb.tangtang.report.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyReportMapperXmlTest {

    private static final String RESOURCE = "mapper/report/MonthlyReportMapper.xml";

    @Test
    @DisplayName("월간 리포트 Mapper XML이 파싱되고 모든 조회문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = MonthlyReportMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findUserCreatedDate"));
        assertTrue(configuration.hasStatement(namespace + ".sumNetSpending"));
        assertTrue(configuration.hasStatement(namespace + ".findMonthlySpending"));
        assertTrue(configuration.hasStatement(namespace + ".findMonthlyCategorySpending"));
        assertTrue(configuration.hasStatement(namespace + ".countActiveFixedExpenseCandidates"));
        assertTrue(configuration.hasStatement(namespace + ".findAiAnalysisSnapshot"));
        assertTrue(configuration.hasStatement(namespace + ".claimAiAnalysisGeneration"));
        assertTrue(configuration.hasStatement(namespace + ".completeAiAnalysis"));
        assertTrue(configuration.hasStatement(namespace + ".failAiAnalysis"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("AS `year_month`"));
        assertTrue(xml.contains("ORDER BY `year_month`"));
        assertTrue(xml.contains("LEFT JOIN tbl_category p ON p.id = c.parent_id"));
        assertTrue(xml.contains("AS parent_category_id"));
        assertTrue(xml.contains("AS parent_category_name"));
        assertTrue(xml.contains("ai_analysis_status IN ('NOT_REQUESTED', 'FAILED')"));
        assertTrue(xml.contains("CAST(#{feedbacksJson} AS JSON)"));
        assertTrue(xml.contains("WHERE user_id = #{userId}"));
        assertTrue(xml.contains("AND `year_month` = #{yearMonth}"));
    }
}

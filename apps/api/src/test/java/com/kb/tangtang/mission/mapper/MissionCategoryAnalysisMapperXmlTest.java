package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionCategoryAnalysisMapperXmlTest {

    private static final String RESOURCE = "mapper/mission/MissionCategoryAnalysisMapper.xml";

    @Test
    @DisplayName("미션 카테고리 분석 Mapper XML이 파싱되고 모든 조회문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = MissionCategoryAnalysisMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".countAllConsumptionTransactions"));
        assertTrue(configuration.hasStatement(namespace + ".countConsumptionTransactions"));
        assertTrue(configuration.hasStatement(namespace + ".sumCategorizedConsumption"));
        assertTrue(configuration.hasStatement(namespace + ".findTopCategorySpending"));
    }

    @Test
    @DisplayName("상대형 후보는 최근 28일 카테고리별 유효 소비 3건과 소비일 2일 이상을 요구한다")
    void requiresMinimumTransactionsAndDistinctDays() throws Exception {
        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(xml.contains("SUM(CASE WHEN t.is_refund = 0 THEN 1 ELSE 0 END) >= 3"));
        assertTrue(xml.contains(
                "COUNT(DISTINCT CASE WHEN t.is_refund = 0 THEN t.tr_date END) >= 2"));
    }
}

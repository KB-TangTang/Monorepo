package com.kb.tangtang.fixedexpense.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseDetectionMapperXmlTest {

    private static final String RESOURCE = "mapper/fixedexpense/FixedExpenseDetectionMapper.xml";

    @Test
    @DisplayName("고정지출 탐지 Mapper XML이 파싱되고 탐지 쿼리가 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = FixedExpenseDetectionMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findActiveRule"));
        assertTrue(configuration.hasStatement(namespace + ".findActiveUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findDetectionTransactions"));
        assertTrue(configuration.hasStatement(namespace + ".findCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".upsertCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".updateDetectedCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".linkTransactionsToCandidate"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("classification = 'CONSUMPTION'"));
        assertTrue(xml.contains("COALESCE(t.cancel_yn, 'N')"));
        assertTrue(xml.contains("t.is_refund = 0"));
        assertTrue(xml.contains("t.is_excluded_from_summary = 0"));
        assertTrue(xml.contains("ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)"));
        assertTrue(xml.contains("AND status = 'ACTIVE'"));
        assertFalse(xml.contains("reactivateBuffer"));
    }
}

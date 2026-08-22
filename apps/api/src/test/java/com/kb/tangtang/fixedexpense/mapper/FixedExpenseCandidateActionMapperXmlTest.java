package com.kb.tangtang.fixedexpense.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseCandidateActionMapperXmlTest {

    private static final String RESOURCE = "mapper/fixedexpense/FixedExpenseCandidateActionMapper.xml";

    @Test
    @DisplayName("고정지출 후보 결정 Mapper XML이 파싱되고 조건부 갱신 쿼리가 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = FixedExpenseCandidateActionMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findOwnedCandidateForUpdate"));
        assertTrue(configuration.hasStatement(namespace + ".confirmCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".excludeCandidate"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("FOR UPDATE"));
        assertTrue(xml.contains("status = 'ACTIVE'"));
        assertTrue(xml.contains("is_excluded = 0"));
        assertTrue(xml.contains("confirmed_at IS NULL"));
        assertTrue(xml.contains("SET confirmed_at = #{confirmedAt}"));
        assertTrue(xml.contains("SET is_excluded = 1"));
    }
}

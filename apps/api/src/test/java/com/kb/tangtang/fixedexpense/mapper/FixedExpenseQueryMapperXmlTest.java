package com.kb.tangtang.fixedexpense.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseQueryMapperXmlTest {

    private static final String RESOURCE = "mapper/fixedexpense/FixedExpenseQueryMapper.xml";

    @Test
    @DisplayName("고정지출 조회 Mapper XML이 파싱되고 활성·소유자 조건을 포함한다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = FixedExpenseQueryMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findActiveItems"));
        assertTrue(configuration.hasStatement(namespace + ".findOwnedActiveItem"));
        assertTrue(configuration.hasStatement(namespace + ".findRecentPaymentHistory"));
        assertTrue(configuration.hasStatement(namespace + ".sumPaymentHistory"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("f.status = 'ACTIVE'"));
        assertTrue(xml.contains("f.is_excluded = 0"));
        assertTrue(xml.contains("f.confirmed_at IS NOT NULL"));
        assertTrue(xml.contains("f.confirmed_at IS NULL"));
        assertTrue(xml.contains("f.user_id = #{userId}"));
        assertTrue(xml.contains("ORDER BY t.tr_date DESC, t.id DESC"));
        assertTrue(xml.contains("LIMIT 6"));
    }
}

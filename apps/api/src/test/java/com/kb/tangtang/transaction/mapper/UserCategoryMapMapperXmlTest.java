package com.kb.tangtang.transaction.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCategoryMapMapperXmlTest {

    private static final String RESOURCE = "mapper/transaction/UserCategoryMapMapper.xml";

    @Test
    @DisplayName("가맹점 규칙 upsert 쿼리가 UserCategoryMapMapper XML에 등록된다")
    void parsesUpsertStatement() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = UserCategoryMapMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findByUserAndMerchant"));
        assertTrue(configuration.hasStatement(namespace + ".upsert"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("ON DUPLICATE KEY UPDATE category_id = VALUES(category_id)"));
    }
}

package com.kb.tangtang.transaction.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMapperXmlTest {

    private static final String RESOURCE = "mapper/transaction/CategoryMapper.xml";

    @Test
    @DisplayName("categoryId 검증용 findById 쿼리가 CategoryMapper XML에 등록된다")
    void parsesFindByIdStatement() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = CategoryMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findAll"));
        assertTrue(configuration.hasStatement(namespace + ".findById"));
    }
}

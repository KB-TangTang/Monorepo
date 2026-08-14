package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionVerdictMapperXmlTest {

    @Test
    void mapperXmlParsesAndRegistersStatements() throws Exception {
        String resource = "mapper/mission/MissionVerdictMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        String namespace = MissionVerdictMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "findOldestUncheckedVerdict"));
        assertTrue(configuration.hasStatement(namespace + "countUncheckedVerdicts"));
        assertTrue(configuration.hasStatement(namespace + "findVerdictTransactions"));
        assertTrue(configuration.hasStatement(namespace + "findResultsThroughDate"));
        assertTrue(configuration.hasStatement(namespace + "countOwnedFinalizedVerdict"));
        assertTrue(configuration.hasStatement(namespace + "acknowledgeVerdict"));
        assertTrue(configuration.hasStatement(namespace + "findResultCheckedAt"));
    }
}

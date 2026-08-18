package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsoluteMissionAssignmentMapperXmlTest {

    @Test
    void mapperXmlParsesAndRegistersStatements() throws Exception {
        String resource = "mapper/mission/AbsoluteMissionAssignmentMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = AbsoluteMissionAssignmentMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".isRelativeMissionQualified"));
        assertTrue(configuration.hasStatement(namespace + ".findNoSpendAbsoluteMissions"));
        assertTrue(configuration.hasStatement(namespace + ".findCategorySpendingStats"));
        assertTrue(configuration.hasStatement(namespace + ".insertAssignment"));
    }
}

package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RelativeMissionAssignmentMapperXmlTest {

    @Test
    void mapperXmlParsesAndRegistersStatements() throws Exception {
        String resource = "mapper/mission/RelativeMissionAssignmentMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = RelativeMissionAssignmentMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findUnassignedChallengeConsentedUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findCategorySpendingStats"));
        assertTrue(configuration.hasStatement(namespace + ".findNoSpendMissions"));
        assertTrue(configuration.hasStatement(namespace + ".insertAssignment"));
    }
}

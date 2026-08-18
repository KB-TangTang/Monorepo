package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionEvaluationMapperXmlTest {

    @Test
    void mapperXmlParsesAndRegistersStatements() throws Exception {
        String resource = "mapper/mission/MissionEvaluationMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        String namespace = MissionEvaluationMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "findPendingAssignmentIds"));
        assertTrue(configuration.hasStatement(namespace + "findPendingAssignmentIdsBefore"));
        assertTrue(configuration.hasStatement(namespace + "lockPendingAssignment"));
        assertTrue(configuration.hasStatement(namespace + "updateMissionResult"));
        assertTrue(configuration.hasStatement(namespace + "increaseSuccessStreak"));
        assertTrue(configuration.hasStatement(namespace + "resetStreak"));
    }
}

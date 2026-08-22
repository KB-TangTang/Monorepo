package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

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
        assertTrue(configuration.hasStatement(namespace + ".findEarliestRecoveryDateBefore"));
        assertTrue(configuration.hasStatement(namespace + ".findUnassignedChallengeConsentedUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findCategorySpendingStats"));
        assertTrue(configuration.hasStatement(namespace + ".insertAssignment"));

        String recoverySql = configuration
                .getMappedStatement(namespace + ".findEarliestRecoveryDateBefore")
                .getBoundSql(Map.of("beforeDate", LocalDate.of(2026, 8, 19)))
                .getSql();
        assertTrue(recoverySql.contains("WITH RECURSIVE"));
        assertTrue(recoverySql.contains("consent.updated_at"));
        assertTrue(recoverySql.contains("assignment.result = 'PENDING'"));
    }
}

package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionAnalysisSnapshotMapperXmlTest {

    private static final String RESOURCE = "mapper/mission/MissionAnalysisSnapshotMapper.xml";

    @Test
    @DisplayName("분석 스냅샷 Mapper XML이 파싱되고 조회·저장문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = MissionAnalysisSnapshotMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findPendingSnapshots"));
        assertTrue(configuration.hasStatement(namespace + ".findLatestCycleSnapshots"));
        assertTrue(configuration.hasStatement(namespace + ".findQualifiedAt"));
        assertTrue(configuration.hasStatement(namespace + ".markQualified"));
        assertTrue(configuration.hasStatement(namespace + ".insertSnapshots"));

        String rotationSql = configuration
                .getMappedStatement(namespace + ".findLatestCycleSnapshots")
                .getBoundSql(Map.of("userId", 1L))
                .getSql();
        assertTrue(rotationSql.contains("'WAITING'"));
        assertTrue(rotationSql.contains("AS rotation_result"));
        assertTrue(rotationSql.contains("AS mission_round"));
        assertTrue(rotationSql.contains("FROM tbl_user_mission_info category_assignment"));
        assertTrue(rotationSql.contains("WHEN analysis.assigned_date IS NULL THEN 1"));

        String pendingSql = configuration
                .getMappedStatement(namespace + ".findPendingSnapshots")
                .getBoundSql(Map.of("userId", 1L, "referenceDate", LocalDate.of(2026, 8, 15)))
                .getSql();
        assertTrue(pendingSql.contains("MIN(latest.cycle_start_date)"));
        assertTrue(pendingSql.contains("latest.assigned_date IS NULL"));
        assertTrue(pendingSql.contains("latest.cycle_start_date <= ?"));

        String assignmentSql = configuration
                .getMappedStatement(namespace + ".findNextPendingSnapshotForUpdate")
                .getBoundSql(Map.of("userId", 1L, "assignDate", LocalDate.of(2026, 8, 15)))
                .getSql();
        assertTrue(assignmentSql.contains("MIN(latest.cycle_start_date)"));
        assertTrue(assignmentSql.contains("latest.assigned_date IS NULL"));
        assertTrue(assignmentSql.contains("latest.cycle_start_date <= ?"));
    }
}

package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionScoreMapperXmlTest {

    @Test
    void mapperXmlRegistersIdempotentMonthlyScoreStatements() throws Exception {
        String resource = "mapper/mission/MissionScoreMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        String namespace = MissionScoreMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "calculateMonthlyScore"));
        assertTrue(configuration.hasStatement(namespace + "upsertMonthlyScore"));
        assertTrue(configuration.hasStatement(namespace + "findMonthlyScore"));
        assertTrue(configuration.hasStatement(namespace + "findTopRankings"));
        assertTrue(configuration.hasStatement(namespace + "findUserRanking"));
        assertTrue(configuration.hasStatement(namespace + "countRankingUsers"));
        assertTrue(configuration.hasStatement(namespace + "findRankingMonths"));

        BoundSql scoreSql = configuration.getMappedStatement(namespace + "calculateMonthlyScore")
                .getBoundSql(Map.of(
                        "userId", 7L,
                        "startDate", LocalDate.of(2026, 8, 1),
                        "endDate", LocalDate.of(2026, 8, 31)));
        assertTrue(scoreSql.getSql().contains("previous_assignment.assign_date = DATE_SUB"));
        assertTrue(scoreSql.getSql().contains("previous_assignment.result = 'SUCCESS'"));
        assertTrue(scoreSql.getSql().contains(
                "CASE WHEN mission.mission_type = 'ABSOLUTE' THEN 50 ELSE difficulty.score END"));
        assertTrue(scoreSql.getSql().contains("JOIN tbl_mission_pool mission"));

        BoundSql upsertSql = configuration.getMappedStatement(namespace + "upsertMonthlyScore")
                .getBoundSql(Map.of("userId", 7L, "yearMonth", "2026-08", "totalScore", 75));
        assertTrue(upsertSql.getSql().contains("ON DUPLICATE KEY UPDATE"));

        BoundSql rankingSql = configuration.getMappedStatement(namespace + "findTopRankings")
                .getBoundSql(Map.of("yearMonth", "2026-08", "limit", 10));
        assertTrue(rankingSql.getSql().contains("ROW_NUMBER() OVER"));
        assertTrue(rankingSql.getSql().contains("u.status = 'ACTIVE'"));

        BoundSql monthsSql = configuration.getMappedStatement(namespace + "findRankingMonths")
                .getBoundSql(Map.of());
        assertTrue(monthsSql.getSql().contains("SELECT DISTINCT"));
        assertTrue(monthsSql.getSql().contains("FROM tbl_monthly_ranking"));
        assertTrue(!monthsSql.getSql().contains("user_id"));
    }
}

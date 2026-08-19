package com.kb.tangtang.report.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeReportMapperXmlTest {

    private static final String RESOURCE = "mapper/report/ChallengeReportMapper.xml";

    @Test
    void parsesMapperXmlAndRegistersStatements() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = ChallengeReportMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".hasActiveChallengeConsent"));
        assertTrue(configuration.hasStatement(namespace + ".findConfirmedReportMonths"));
        assertTrue(configuration.hasStatement(namespace + ".findFirstMissionMonth"));
        assertTrue(configuration.hasStatement(namespace + ".findChallengeConsentMonth"));
        assertTrue(configuration.hasStatement(namespace + ".findFinalizedReportUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findFinalizedMissionRows"));
        assertTrue(configuration.hasStatement(namespace + ".findFirstMissionDate"));
        assertTrue(configuration.hasStatement(namespace + ".findDifficultyPolicies"));
        assertTrue(configuration.hasStatement(namespace + ".upsertMonthlyReport"));
        assertTrue(configuration.hasStatement(namespace + ".updateMonthlyGroupRecord"));
        assertTrue(configuration.hasStatement(namespace + ".findMonthEndClosedGroupReportUserIds"));
        assertTrue(configuration.hasStatement(namespace + ".findMonthlyReport"));
        assertTrue(configuration.hasStatement(namespace + ".findPreviousMonthlyReport"));
        assertTrue(configuration.hasStatement(namespace + ".hasJudgingGroupRecord"));
        assertTrue(configuration.hasStatement(namespace + ".findGroupRecord"));

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            String xml = new String(inputStream.readAllBytes());
            assertFalse(xml.contains("${"));
            assertTrue(xml.contains("consent_type = 'CHALLENGE'"));
            assertTrue(xml.contains("tbl_challenge_monthly_report"));
            assertTrue(xml.contains("`year_month` &lt; #{currentYearMonth}"));
            assertTrue(xml.contains("ORDER BY `year_month` DESC"));
            assertTrue(xml.contains("tbl_user_mission_info"));
            assertTrue(xml.contains("assign_date &lt; #{currentMonthStart}"));
            assertTrue(xml.contains("created_at &lt; #{currentMonthStart}"));
            assertTrue(xml.contains("assignment.result IN ('SUCCESS', 'FAIL')"));
            assertTrue(xml.contains("assignment.base_amount IS NOT NULL"));
            assertTrue(xml.contains("<if test=\"!includeExisting\">"));
            assertTrue(xml.contains("mission_transaction.classification = 'CONSUMPTION'"));
            assertTrue(xml.contains("mission_transaction.is_excluded_from_summary = 0"));
            assertTrue(xml.contains("saved_amount"));
            assertTrue(xml.contains("over_amount"));
            assertTrue(xml.contains("category_results_json"));
            assertTrue(xml.contains("over_amount AS overspent_amount"));
            assertTrue(xml.contains("category_results_json AS category_effects_json"));
            assertTrue(xml.contains("SELECT MIN(assign_date)"));
            assertTrue(xml.contains("mission.mission_type"));
            assertTrue(xml.contains(
                    "CASE WHEN mission.mission_type = 'ABSOLUTE' THEN 50 ELSE difficulty.score END"));
            assertFalse(xml.contains("mission_type = 'RELATIVE'"));
            assertTrue(xml.contains("CAST(#{weeklyResultsJson} AS JSON)"));
            assertTrue(xml.contains("CAST(#{groupRecordJson} AS JSON)"));
            assertTrue(xml.contains("group_record_json"));
            assertTrue(xml.contains("ON DUPLICATE KEY UPDATE"));
            assertTrue(xml.contains("UPDATE tbl_challenge_monthly_report"));
            /*
             * 보강 UPDATE 의 WHERE 는 user_id + year_month 여야 한다 (이슈 #267).
             * 여기에 조건이 하나라도 더 붙으면 — 예컨대 group_record_json IS NULL 로 좁히면 —
             * 확정 이벤트와 보강 cron 이 겹쳐 돌 때 두 번째 호출이 조용히 0행이 되어 멱등이 깨진다.
             * 반대로 조건이 빠지면 남의 달 리포트를 덮는다.
             */
            assertTrue(xml.contains("SET group_record_json = CAST(#{groupRecordJson} AS JSON)\n"
                    + "         WHERE user_id = #{userId}\n"
                    + "           AND `year_month` = #{yearMonth}"));
            assertTrue(xml.contains("g.end_date = #{endDate}"));
            assertTrue(xml.contains("g.end_date BETWEEN #{startDate} AND #{endDate}"));
            assertTrue(xml.contains("g.status = 'CLOSED'"));
            /*
             * 'JUDGING' 단독으로 좁히면 월 마지막 날에 끝난 그룹이 다음 달 1일 하루 동안
             * EMPTY(전적 없음)로 떨어진다 — ACTIVE → JUDGING 전이가 종료 다음다음 날이라
             * 그날은 아직 ACTIVE 다(이슈 #169). 화면에 오류가 안 뜨는 종류의 회귀라 못박는다.
             */
            assertTrue(xml.contains("g.status IN ('ACTIVE', 'JUDGING')"));
            assertFalse(xml.contains("finalized_member.final_outcome IS NULL"));
            assertFalse(xml.contains("pending_indictment.status IN ('DEFENSE_WAIT', 'VOTING')"));
            assertTrue(xml.contains("indictment.result IS NOT NULL"));
            assertTrue(xml.contains("indictment.result = 0"));
            assertTrue(xml.contains("indictment.result = 1"));
        }
    }
}

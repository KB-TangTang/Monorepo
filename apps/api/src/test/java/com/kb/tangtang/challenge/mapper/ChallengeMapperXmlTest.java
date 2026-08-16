package com.kb.tangtang.challenge.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 매퍼 XML 이 파싱되는지, 인터페이스의 메서드가 전부 등록되는지 본다.
 *
 * XML 한 글자가 틀리면 sqlSessionFactory 생성이 실패하면서 컨텍스트가 통째로 뜨지 않는다.
 * DB 없이 잡을 수 있는 오류라 여기서 막는다.
 */
class ChallengeMapperXmlTest {

    private Configuration parse(String resource) throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource,
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    @Test
    @DisplayName("ChallengeGroupMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesChallengeGroupMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/ChallengeGroupMapper.xml");

        String namespace = ChallengeGroupMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".insertGroup"));
        assertTrue(configuration.hasStatement(namespace + ".findById"));
        assertTrue(configuration.hasStatement(namespace + ".findByInviteCode"));
        assertTrue(configuration.hasStatement(namespace + ".findMyGroups"));
        assertTrue(configuration.hasStatement(namespace + ".countByInviteCode"));
        assertTrue(configuration.hasStatement(namespace + ".findGroupsToStart"));
        assertTrue(configuration.hasStatement(namespace + ".findGroupsToEvaluate"));
        assertTrue(configuration.hasStatement(namespace + ".findGroupsToJudge"));
        assertTrue(configuration.hasStatement(namespace + ".updateStatusIfCurrent"));
        assertTrue(configuration.hasStatement(namespace + ".deleteIfCurrent"));
    }

    /**
     * DELETE 에서 조건절이 빠지면 정상 시작한 그룹까지 CASCADE 로 지워진다(이슈 #261).
     * 되돌릴 수 없는 사고라 SQL 모양 자체를 못박는다.
     */
    @Test
    @DisplayName("미성립 그룹 삭제는 status 조건 없이 실행되지 않는다")
    void deleteIfCurrentKeepsStatusGuard() throws Exception {
        Configuration configuration = parse("mapper/challenge/ChallengeGroupMapper.xml");

        String sql = configuration
                .getMappedStatement(ChallengeGroupMapper.class.getName() + ".deleteIfCurrent")
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql();

        assertTrue(sql.replaceAll("\\s+", " ").contains("status = ?"),
                "조건절이 없으면 그 사이 ACTIVE 로 전이된 그룹을 통째로 지운다");
    }

    /**
     * 부등호가 {@code <=} 로 느슨해지면 종료 다음 날 자정에 JUDGING 으로 넘어가고,
     * 평가·기소 배치(이슈 #168)가 {@code status = 'ACTIVE'} 로 조회하는 그 그룹이 사라져
     * <b>챌린지 마지막 날치 기소가 통째로 안 생긴다.</b> 화면에는 아무 오류도 안 뜬다.
     */
    @Test
    @DisplayName("재판 전이 조회는 end_date 를 등호 없이 비교한다")
    void findGroupsToJudgeExcludesTheBoundaryDay() throws Exception {
        Configuration configuration = parse("mapper/challenge/ChallengeGroupMapper.xml");

        String sql = configuration
                .getMappedStatement(ChallengeGroupMapper.class.getName() + ".findGroupsToJudge")
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql()
                .replaceAll("\\s+", " ");

        assertTrue(sql.contains("g.end_date < ?"),
                "<= 로 바꾸면 평가 배치가 마지막 날 기소를 만들기 전에 그룹이 JUDGING 으로 빠진다");
    }

    @Test
    @DisplayName("GroupMemberMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesGroupMemberMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupMemberMapper.xml");

        String namespace = GroupMemberMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".insertMember"));
        assertTrue(configuration.hasStatement(namespace + ".findByGroupIds"));
    }

    @Test
    @DisplayName("GroupChallengeResultMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesGroupChallengeResultMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String namespace = GroupChallengeResultMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".upsertDailyResults"));
        assertTrue(configuration.hasStatement(namespace + ".findOverLimitDaily"));
        assertTrue(configuration.hasStatement(namespace + ".findOverLimitPeriod"));
        assertTrue(configuration.hasStatement(namespace + ".findDeductionOverflow"));
        assertTrue(configuration.hasStatement(namespace + ".findMemberConsumption"));
    }

    /**
     * UPSERT 의 UPDATE 절에 판결 전용 컬럼이 섞여 들어가는 것을 막는다.
     *
     * 들어가도 SQL 은 정상 실행되고 화면도 멀쩡해 보인다. 다만 무죄 판결로 인정된 감액이
     * 5분마다 0 으로 초기화될 뿐이다 — 리뷰로 잡기 어려운 회귀라 테스트로 못박는다.
     * ({@code db/migration/20260814_group_challenge_verdict_deduction.sql})
     */
    @Test
    @DisplayName("일별 집계 UPSERT 는 판결 전용 컬럼을 쓰지 않는다")
    void upsertDoesNotTouchVerdictColumns() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String sql = configuration
                .getMappedStatement(GroupChallengeResultMapper.class.getName() + ".upsertDailyResults")
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql();

        assertFalse(sql.contains("verdict_deduction_amount"),
                "UPSERT 가 verdict_deduction_amount 를 건드리면 무죄 감액이 5분마다 사라진다");
        assertFalse(sql.contains("effective_amount"),
                "effective_amount 는 생성 컬럼이라 INSERT·UPDATE 대상이 될 수 없다");
    }

    @Test
    @DisplayName("IndictmentMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesIndictmentMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String namespace = IndictmentMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".insertIndictment"));
        assertTrue(configuration.hasStatement(namespace + ".findDefenseTodos"));
        assertTrue(configuration.hasStatement(namespace + ".findVoteTodos"));
        assertTrue(configuration.hasStatement(namespace + ".findOpenByGroupId"));
        assertTrue(configuration.hasStatement(namespace + ".findTrialSummaryByGroupIds"));
    }

    /**
     * 투표 대기 조회에서 참여자 조인이 빠지면 <b>남의 그룹 재판이 내 할 일 목록에 뜨고</b>
     * 그 링크로 투표 화면까지 열린다. 조회 결과만 보고는 눈치채기 어려운 권한 구멍이라 못박는다.
     */
    @Test
    @DisplayName("투표 대기 조회는 참여자 조인 없이 실행되지 않는다")
    void voteTodosKeepMembershipJoin() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = configuration
                .getMappedStatement(IndictmentMapper.class.getName() + ".findVoteTodos")
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql()
                .replaceAll("\\s+", " ");

        assertTrue(sql.contains("JOIN tbl_group_member m ON m.group_id = i.group_id"),
                "조인이 없으면 내가 속하지 않은 그룹의 재판이 할 일 목록에 섞인다");
    }
}

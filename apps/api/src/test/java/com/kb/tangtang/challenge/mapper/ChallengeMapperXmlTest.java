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
        assertTrue(configuration.hasStatement(namespace + ".findUserIdsByGroupId"));
        assertTrue(configuration.hasStatement(namespace + ".decreaseLife"));
    }

    /**
     * {@code lives_count} 는 UNSIGNED 가 아니라 그냥 INT 다. 조건이 빠지면 −1 이 그대로 저장되고,
     * 종료 후 확정 배치의 「{@code lives_count = 0} 이면 탈락」 판정이 <b>조용히 빗나간다</b> —
     * 탈락해야 할 사람이 생존자로 순위에 오른다.
     */
    @Test
    @DisplayName("목숨 차감은 0 이하로 내려가지 않는다")
    void decreaseLifeGuardsAgainstNegative() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupMemberMapper.xml");

        String sql = sqlOf(configuration, GroupMemberMapper.class.getName() + ".decreaseLife");

        assertTrue(sql.contains("lives_count > 0"),
                "조건이 없으면 목숨이 음수가 되어 탈락 판정이 어긋난다");
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
        assertTrue(configuration.hasStatement(namespace + ".findTrialTransactions"));
        assertTrue(configuration.hasStatement(namespace + ".addVerdictDeduction"));
    }

    /**
     * 무죄 감액은 <b>대입이 아니라 누적</b>이어야 한다. 기간평가는 기간 전체의 기소가
     * {@code end_date} 행 한 줄에 붙고 일일평가도 같은 행이 다시 기소될 수 있어, 대입으로 쓰면
     * 앞선 무죄의 감액이 사라진다.
     *
     * <p>{@code effective_amount} 는 STORED 생성 컬럼이라 SET 절에 넣는 순간 UPDATE 가 죽는다.
     */
    @Test
    @DisplayName("판결 감액은 누적이고 생성 컬럼을 건드리지 않는다")
    void verdictDeductionAccumulates() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String sql = sqlOf(configuration,
                GroupChallengeResultMapper.class.getName() + ".addVerdictDeduction");

        assertTrue(sql.contains("verdict_deduction_amount = verdict_deduction_amount + ?"),
                "대입으로 바꾸면 같은 행의 앞선 무죄 감액이 지워진다");
        assertFalse(sql.contains("effective_amount"),
                "effective_amount 는 생성 컬럼이라 UPDATE 대상이 될 수 없다");
    }

    /**
     * 변론 화면 거래 목록과 집계 배치가 같은 조건을 쓰는지 본다.
     *
     * <p>어긋나면 「합계는 36,700원인데 목록을 더하면 31,700원」 이 되고, 그 합계가 건별
     * 실제부담금 입력의 상한이라 사용자가 정당한 금액을 신고할 수 없게 된다.
     * 조건을 복사해 두면 한쪽만 바뀌어도 아무 오류가 나지 않는 회귀라 못박는다.
     */
    @Test
    @DisplayName("변론 화면 거래 목록은 집계와 같은 소비 조건 · 환불 규칙을 쓴다")
    void trialTransactionsShareConsumptionRules() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String namespace = GroupChallengeResultMapper.class.getName();
        String list = sqlOf(configuration, namespace + ".findTrialTransactions");
        String upsert = sqlOf(configuration, namespace + ".upsertDailyResults");

        String filter = "AND t.classification = 'CONSUMPTION' AND t.direction = 'OUT' "
                + "AND t.is_excluded_from_summary = 0 "
                + "AND (g.category_id IS NULL OR t.category_id = g.category_id)";
        assertTrue(upsert.contains(filter), "consumptionFilter 조각의 모양이 바뀌었다");
        assertTrue(list.contains(filter), "목록이 집계와 다른 조건으로 거래를 세면 합계가 어긋난다");

        String refundRule = "-COALESCE(t.refunded_amount, t.amount)";
        assertTrue(upsert.contains(refundRule), "환불 음수화 규칙의 모양이 바뀌었다");
        assertTrue(list.contains(refundRule), "목록이 환불을 음수로 내리지 않으면 더해도 합계가 안 나온다");
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
        assertTrue(configuration.hasStatement(namespace + ".findTrialDetail"));
        assertTrue(configuration.hasStatement(namespace + ".moveToVoting"));
        assertTrue(configuration.hasStatement(namespace + ".confirmConfession"));
        assertTrue(configuration.hasStatement(namespace + ".moveExpiredDefensesToVoting"));
        assertTrue(configuration.hasStatement(namespace + ".findVotingToTally"));
        assertTrue(configuration.hasStatement(namespace + ".confirmVerdict"));
    }

    @Test
    @DisplayName("DefenseMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesDefenseMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/DefenseMapper.xml");

        String namespace = DefenseMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".insertDefense"));
        assertTrue(configuration.hasStatement(namespace + ".insertDefenseImages"));
        assertTrue(configuration.hasStatement(namespace + ".existsByIndictmentId"));
        assertTrue(configuration.hasStatement(namespace + ".findByIndictmentId"));
        assertTrue(configuration.hasStatement(namespace + ".findImageKeysByDefenseId"));
    }

    /**
     * 상태 전이 세 구문의 {@code WHERE status = 'DEFENSE_WAIT'} 는 <b>동시 요청 방어이자
     * 멱등의 근거</b>다. 이 조건이 빠지면 이미 확정된 판결(GUILTY)을 변론 제출이 되돌려 쓰고,
     * 마감 배치는 두 번 돌 때마다 투표 중인 재판을 다시 건드린다.
     *
     * <p>서비스는 이 구문이 <b>0행을 돌려주는 것</b>에 의존해 예외로 롤백한다
     * ({@code DefenseService#registerDefense}). 조건이 사라지면 그 방어가 통째로 무력해진다.
     */
    @Test
    @DisplayName("상태 전이 구문은 DEFENSE_WAIT 조건 없이 실행되지 않는다")
    void stateTransitionsGuardCurrentStatus() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String namespace = IndictmentMapper.class.getName();
        for (String id : new String[] {"moveToVoting", "confirmConfession",
                "moveExpiredDefensesToVoting"}) {
            assertTrue(sqlOf(configuration, namespace + "." + id).contains("status = 'DEFENSE_WAIT'"),
                    id + " 에 현재 상태 조건이 없으면 확정된 판결을 되돌려 쓰거나 멱등이 깨진다");
        }
    }

    /**
     * {@code ck_ind_result} 는 {@code GUILTY} 에 {@code result = 1} 조합만 허용한다
     * ({@code db/schema.sql} 의 CHECK). {@code result} 를 빼면 INSERT 가 아니라 UPDATE 단계에서
     * DB 가 거부해 혐의 인정이 통째로 실패한다.
     */
    @Test
    @DisplayName("혐의 인정은 status·result·verdict_method 를 함께 쓴다")
    void confessionSatisfiesResultCheck() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = sqlOf(configuration, IndictmentMapper.class.getName() + ".confirmConfession");

        assertTrue(sql.contains("status = 'GUILTY'"), "혐의 인정은 유죄 확정이다");
        assertTrue(sql.contains("result = 1"), "result 를 빼면 ck_ind_result 가 UPDATE 를 거부한다");
        assertTrue(sql.contains("verdict_method = 'CONFESSION'"),
                "판결 방식이 없으면 개표 결과와 자진 인정을 구분할 수 없다");
    }

    /**
     * 마감 판정은 {@code created_at + defense-hours} 다. 시간 값이 프로퍼티라
     * SQL 에 상수로 박을 수 없어 파라미터로 받는다 — {@code ${}} 는 금지 대상이다.
     */
    @Test
    @DisplayName("변론 마감 배치는 마감 시간을 파라미터로 받는다")
    void deadlineBatchBindsDefenseHours() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = sqlOf(configuration,
                IndictmentMapper.class.getName() + ".moveExpiredDefensesToVoting");

        assertTrue(sql.contains("INTERVAL ? HOUR"),
                "시간 값이 SQL 에 상수로 박히면 프로퍼티를 바꿔도 배치가 따라오지 않는다");
    }

    /**
     * 개표 대상은 「투표 마감이 지났다 <b>OR</b> 투표할 사람이 전부 던졌다」 둘 다다(이슈 #172 결정 2).
     *
     * <p>앞쪽만 남으면 전원이 투표를 끝내고도 최대 30시간을 기다려야 하고, 뒤쪽만 남으면
     * <b>한 명이라도 투표를 안 하면 재판이 영원히 끝나지 않는다.</b> 어느 쪽이 빠져도 화면에는
     * 아무 오류가 뜨지 않고 「투표 완료」인 채로 멈춰 있을 뿐이라 SQL 모양으로 못박는다.
     */
    @Test
    @DisplayName("개표 대상 조회는 마감 경과와 전원 투표를 모두 건다")
    void tallyPicksDeadlinePassedOrEveryoneVoted() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = sqlOf(configuration, IndictmentMapper.class.getName() + ".findVotingToTally");

        assertTrue(sql.contains("i.status = 'VOTING'"),
                "VOTING 만 잡아야 혐의 인정·확정된 재판이 자동으로 빠진다");

        /* 변론 + 투표 두 구간이라 파라미터가 둘이다. 하나만 남으면 마감 시점이 어긋난다 */
        int first = sql.indexOf("INTERVAL ? HOUR");
        assertTrue(first >= 0 && sql.indexOf("INTERVAL ? HOUR", first + 1) > first,
                "변론·투표 시간을 각각 파라미터로 받지 않으면 마감 시점이 화면과 어긋난다");

        assertTrue(sql.contains("(SELECT COUNT(*) FROM tbl_vote v WHERE v.indictment_id = i.id) >="),
                "던진 표 수를 세지 않으면 조기 확정이 사라져 전원 투표해도 30시간을 기다린다");
        assertTrue(sql.contains(
                        "(SELECT COUNT(*) FROM tbl_group_member gm WHERE gm.group_id = i.group_id) - 1"),
                "분모에서 피고를 빼지 않으면 전원이 투표해도 조건이 성립하지 않는다");
    }

    /**
     * {@code WHERE status = 'VOTING'} 이 개표의 <b>멱등 근거</b>다. 배치를 두 번 돌리거나 두 인스턴스가
     * 같은 틱을 잡으면 두 번째가 0행을 바꾸고, 서비스는 그 0 을 보고 뒤처리를 건너뛴다
     * ({@code GroupVerdictTransitionService#confirm}). 조건이 빠지면 <b>목숨이 두 번 깎이고</b>
     * 감액이 두 번 더해지며 알림도 두 번 나간다.
     */
    @Test
    @DisplayName("판결 확정 UPDATE 는 VOTING 조건 없이 실행되지 않는다")
    void confirmVerdictGuardsVotingStatus() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = sqlOf(configuration, IndictmentMapper.class.getName() + ".confirmVerdict");

        assertTrue(sql.contains("status = 'VOTING'"),
                "조건이 없으면 배치가 두 번 돌 때 목숨이 두 번 깎인다");
        assertTrue(sql.contains("result = ?"),
                "result 를 빼면 ck_ind_result 가 UPDATE 를 거부한다");
        assertTrue(sql.contains("ai_verdict_reason = ?"),
                "사유를 싣지 않으면 탕이 판결 화면에 근거가 비어 보인다");
    }

    /**
     * 재판 상세에서 참여자 조인이 빠지면 <b>기소 ID 만 알면 남의 그룹 재판 상세가 열린다.</b>
     * 피고 닉네임·소비 금액이 그대로 노출되고, 이 응답은 투표 화면(#171)도 함께 쓴다.
     */
    @Test
    @DisplayName("재판 상세 조회는 참여자 조인 없이 실행되지 않는다")
    void trialDetailKeepsMembershipJoin() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = configuration
                .getMappedStatement(IndictmentMapper.class.getName() + ".findTrialDetail")
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql()
                .replaceAll("\\s+", " ");

        assertTrue(sql.contains("JOIN tbl_group_member gm ON gm.group_id = i.group_id"),
                "조인이 없으면 그룹 밖 사람이 남의 재판 상세를 열 수 있다");
    }

    /**
     * 기간평가 소비액 공식이 세 곳에 같은 모양으로 있는지 본다.
     *
     * <p><b>왜 문자열로 검사하나</b> — 상관 서브쿼리가 참조하는 바깥 별칭이 호출부마다 달라
     * ({@code r_end.user_id} · {@code m.user_id} · {@code i.user_id}) {@code <sql>} 조각 하나로
     * 묶으려면 {@code ${}} 를 써야 하고 그것은 금지 대상이다. 대신 내부 별칭을 {@code rp} 로
     * 통일해 두었으니, 한 곳만 고치면 이 테스트가 깨지게 만든다.
     *
     * <p>깨졌을 때 고치는 곳은 {@code GroupChallengeResultMapper.xml} 의
     * 「★ 기간평가(PERIOD) 소비액 공식」 주석이다. 그 주석이 원본이다.
     */
    @Test
    @DisplayName("기간평가 소비액 공식이 세 구문에 같은 모양으로 들어 있다")
    void periodConsumptionFormulaStaysIdentical() throws Exception {
        String formula = "GREATEST(SUM(rp.daily_amount - rp.verdict_deduction_amount), 0)";

        Configuration results = parse("mapper/challenge/GroupChallengeResultMapper.xml");
        Configuration indictments = parse("mapper/challenge/IndictmentMapper.xml");

        String resultNamespace = GroupChallengeResultMapper.class.getName();
        assertTrue(sqlOf(results, resultNamespace + ".findOverLimitPeriod").contains(formula),
                "기간평가 기소 판정이 무죄 감액을 빼지 않으면 무죄받은 사람을 다시 기소한다");
        assertTrue(sqlOf(results, resultNamespace + ".findMemberConsumption").contains(formula),
                "그룹 상세 소비액이 감액을 반영하지 않으면 무죄 판결이 화면에 나타나지 않는다");
        assertTrue(sqlOf(indictments, IndictmentMapper.class.getName() + ".findTrialDetail")
                        .contains(formula),
                "재판 상세의 초과액·소비액이 다른 화면과 어긋난다");
    }

    /**
     * 행마다 클램프하는 {@code effective_amount} 로 기간 합계를 구하면 환불 상쇄용 음수 행이
     * 0 으로 깎여 <b>환불받은 소비로 기소된다.</b> 합산한 뒤 한 번만 클램프해야 한다.
     */
    @Test
    @DisplayName("기간평가 합계는 행별 클램프 컬럼을 쓰지 않는다")
    void periodSumDoesNotUseEffectiveAmount() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String sql = sqlOf(configuration,
                GroupChallengeResultMapper.class.getName() + ".findOverLimitPeriod");

        assertFalse(sql.contains("SUM(rp.effective_amount)"),
                "SUM(effective_amount) 는 환불 음수 행을 0 으로 깎아 합계를 부풀린다");
    }

    /**
     * 감액 이상 경고가 정상 데이터에서 뜨지 않게 두 조건을 못박는다.
     *
     * <ul>
     *   <li>{@code eval_type = 'DAILY'} — 기간평가는 기간 전체 감액을 end_date 행 한 줄에 적으므로
     *       {@code verdict_deduction_amount > daily_amount} 가 정상이다.
     *   <li>{@code verdict_deduction_amount > 0} — 환불이 소비보다 많은 날은 {@code daily_amount} 가
     *       음수가 되어, 감액한 적이 없는 행도 {@code 0 > -10000} 으로 걸린다.
     * </ul>
     *
     * <p>둘 다 2026-08-18 수동 검증에서 실제로 오탐이 나 추가한 조건이다. 빠지면 경고 로그가
     * 정상 데이터로 가득 차 아무도 안 보게 된다.
     */
    @Test
    @DisplayName("감액 이상 조회는 일일평가·감액 있는 행으로 좁혀져 있다")
    void deductionOverflowSkipsNormalRows() throws Exception {
        Configuration configuration = parse("mapper/challenge/GroupChallengeResultMapper.xml");

        String sql = sqlOf(configuration,
                GroupChallengeResultMapper.class.getName() + ".findDeductionOverflow");

        assertTrue(sql.contains("g.eval_type = 'DAILY'"),
                "기간평가는 감액이 그날 금액을 넘는 게 정상이라 오탐이 뜬다");
        assertTrue(sql.contains("r.verdict_deduction_amount > 0"),
                "환불로 daily_amount 가 음수인 날은 감액 0 인 행까지 경고에 걸린다");
    }

    private String sqlOf(Configuration configuration, String statementId) {
        return configuration.getMappedStatement(statementId)
                .getBoundSql(new java.util.HashMap<String, Object>())
                .getSql()
                .replaceAll("\\s+", " ");
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

    /* ══ 투표 (이슈 #171) ═══════════════════════════════════════════════ */

    @Test
    @DisplayName("VoteMapper XML 이 파싱되고 모든 구문이 등록된다")
    void parsesVoteMapper() throws Exception {
        Configuration configuration = parse("mapper/challenge/VoteMapper.xml");

        String namespace = VoteMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".insert"));
        assertTrue(configuration.hasStatement(namespace + ".existsByIndictmentIdAndUserId"));
        assertTrue(configuration.hasStatement(namespace + ".findCommentsByIndictmentId"));
    }

    /**
     * 표 한 장은 {@code (group_id, user_id, indictment_id)} 복합 PK 로 식별된다.
     * {@code group_id} 가 빠지면 NOT NULL 로 INSERT 자체가 죽고,
     * {@code ON DUPLICATE KEY UPDATE} 가 붙으면 <b>투표 수정이 조용히 생긴다</b> —
     * 마감 직전 눈치싸움을 막으려고 일부러 없앤 경로다(이슈 #171 결정).
     */
    @Test
    @DisplayName("투표 INSERT 는 복합 PK 3개를 모두 쓰고 갱신 경로가 없다")
    void voteInsertHasNoUpdatePath() throws Exception {
        Configuration configuration = parse("mapper/challenge/VoteMapper.xml");

        String sql = sqlOf(configuration, VoteMapper.class.getName() + ".insert");

        assertTrue(sql.contains("group_id"), "복합 PK 의 일부라 빠지면 INSERT 가 죽는다");
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("indictment_id"));
        assertFalse(sql.contains("ON DUPLICATE KEY"),
                "UPSERT 로 바뀌면 재투표가 열려 개표 직전 표를 갈아탈 수 있다");
    }

    /**
     * 코멘트는 <b>익명</b>이다. {@code tbl_user} 를 조인하거나 {@code user_id} 를 뽑는 순간
     * 「누가 뭐라고 했는지」가 응답에 실린다. 정렬도 {@code created_at} 뿐이다 —
     * {@code user_id} 를 tie-breaker 로 넣으면 순서에서 투표자가 읽힌다.
     */
    @Test
    @DisplayName("판결 코멘트 조회는 투표자를 식별할 수 있는 컬럼을 뽑지 않는다")
    void voteCommentsStayAnonymous() throws Exception {
        Configuration configuration = parse("mapper/challenge/VoteMapper.xml");

        String sql = sqlOf(configuration,
                VoteMapper.class.getName() + ".findCommentsByIndictmentId");

        assertFalse(sql.contains("tbl_user"), "닉네임을 붙이는 순간 익명이 아니다");
        assertFalse(sql.contains("user_id"), "user_id 가 내려가면 화면이 투표자를 특정할 수 있다");
        assertFalse(sql.contains("verdict"), "코멘트 옆에 유무죄가 붙으면 코멘트가 곧 표가 된다");
        assertTrue(sql.contains("TRIM(comment) <> ''"),
                "공백만 남은 코멘트가 목록에 빈 줄로 뜬다");
    }

    /**
     * 표 분포는 SQL 이 <b>항상</b> 센다. 가리는 일은 {@code GroupTrialService#isCounted} 한 곳이
     * 맡는다 — SQL 을 상태별로 나누면 이슈 #172 가 개표를 공개할 때 조건을 두 군데서 풀어야 하고
     * 한쪽을 빠뜨리면 투표 중에 비율이 새어 편승 투표가 생긴다.
     */
    @Test
    @DisplayName("재판 상세는 유죄·무죄 표수를 상태와 무관하게 센다")
    void trialDetailCountsBothVerdicts() throws Exception {
        Configuration configuration = parse("mapper/challenge/IndictmentMapper.xml");

        String sql = sqlOf(configuration, IndictmentMapper.class.getName() + ".findTrialDetail");

        assertTrue(sql.contains("v.verdict = 'GUILTY') AS guilty_count"),
                "guilty_count 가 없으면 개표 후 화면이 표 분포를 그릴 수 없다");
        assertTrue(sql.contains("v.verdict = 'INNOCENT') AS innocent_count"),
                "innocent_count 가 없으면 개표 후 화면이 표 분포를 그릴 수 없다");
        assertFalse(sql.contains("i.status = 'GUILTY'"),
                "노출 조건을 SQL 에 넣으면 #172 가 두 군데를 풀어야 한다");
    }
}

package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictMethod;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 개표 규칙 (이슈 #172, 요구사항정의서 6.5).
 *
 * <p><b>DB 도 Mockito 도 쓰지 않는다.</b> 개표는 재판 결과를 되돌릴 수 없게 만드는 지점이라
 * 가장 촘촘히 검증돼야 하는데, 판정을 배치나 매퍼에 섞어 두면 그 검증에 DB 가 필요해진다.
 * 순수 함수로 떼어 뒀기 때문에 이 파일이 다섯 분기를 전부 짚는다.
 */
class VerdictDeciderTest {

    private static VerdictTallyRow tally(int guilty, int innocent, int totalVoters) {
        VerdictTallyRow row = new VerdictTallyRow();
        row.setIndictmentId(1L);
        row.setGuiltyCount(guilty);
        row.setInnocentCount(innocent);
        row.setTotalVoters(totalVoters);
        return row;
    }

    @Test
    @DisplayName("유죄가 많으면 다수결 유죄")
    void guiltyWins() {
        VerdictDecision decision = VerdictDecider.decide(tally(3, 1, 4));

        assertFalse(decision.needsAi());
        assertEquals(IndictmentStatus.GUILTY, decision.getStatus());
        assertEquals(VerdictMethod.VOTE, decision.getMethod());
        /* ck_ind_result 가 GUILTY + result=1 조합만 허용한다 */
        assertEquals(1, decision.getResult());
    }

    @Test
    @DisplayName("무죄가 많으면 다수결 무죄")
    void innocentWins() {
        VerdictDecision decision = VerdictDecider.decide(tally(1, 3, 4));

        assertFalse(decision.needsAi());
        assertEquals(IndictmentStatus.INNOCENT, decision.getStatus());
        assertEquals(VerdictMethod.VOTE, decision.getMethod());
        assertEquals(0, decision.getResult());
    }

    /**
     * 아무도 투표하지 않은 채 마감된 재판. 무죄 추정 원칙이라 무죄지만 {@code VOTE} 가 아니라
     * {@code NO_VOTE} 다 — 화면이 「표로 무죄를 받았다」와 「아무도 안 왔다」를 구분해야 한다.
     */
    @Test
    @DisplayName("0표면 무죄 추정 — NO_VOTE")
    void noVoteFallsBackToInnocent() {
        VerdictDecision decision = VerdictDecider.decide(tally(0, 0, 3));

        assertFalse(decision.needsAi());
        assertEquals(IndictmentStatus.INNOCENT, decision.getStatus());
        assertEquals(VerdictMethod.NO_VOTE, decision.getMethod());
    }

    @Test
    @DisplayName("동률(0 초과)이면 판사 탕이에게 위임한다")
    void tieDelegatesToAi() {
        VerdictDecision decision = VerdictDecider.decide(tally(2, 2, 4));

        assertTrue(decision.needsAi());
        assertEquals(VerdictMethod.AI_JUDGMENT, decision.getMethod());
    }

    /**
     * 피고 혼자인 그룹은 표가 들어올 수 없다. 별도 분기를 두지 않고 0:0 으로 떨어뜨리는 것이
     * 의도다 — 「아무도 유죄라고 하지 않았다」는 점에서 무투표와 같은 상황이다.
     * 여기가 깨지면 그런 재판이 동률로 분류돼 <b>매 틱 LLM 을 두드린다.</b>
     */
    @Test
    @DisplayName("투표할 사람이 없는 그룹도 동률이 아니라 NO_VOTE 다")
    void soloGroupIsNoVoteNotTie() {
        VerdictDecision decision = VerdictDecider.decide(tally(0, 0, 0));

        assertFalse(decision.needsAi());
        assertEquals(IndictmentStatus.INNOCENT, decision.getStatus());
        assertEquals(VerdictMethod.NO_VOTE, decision.getMethod());
    }
}

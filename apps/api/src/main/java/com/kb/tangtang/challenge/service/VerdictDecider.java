package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;

/**
 * 표를 세어 판결을 정한다 (이슈 #172).
 *
 * <p><b>DB 도 스프링도 모른다.</b> 개표 규칙은 재판 결과를 되돌릴 수 없게 만드는 부분이라
 * 가장 촘촘히 검증돼야 하는데, 배치·매퍼에 섞어 두면 그 검증에 DB 가 필요해진다.
 * 순수 함수로 떼어 두면 {@code VerdictDeciderTest} 가 다섯 분기를 전부 짚는다.
 *
 * <table>
 *   <caption>개표 규칙 (요구사항정의서 6.5)</caption>
 *   <tr><th>표</th><th>결과</th><th>verdict_method</th></tr>
 *   <tr><td>유죄 &gt; 무죄</td><td>유죄</td><td>VOTE</td></tr>
 *   <tr><td>무죄 &gt; 유죄</td><td>무죄</td><td>VOTE</td></tr>
 *   <tr><td>0 : 0</td><td>무죄(추정)</td><td>NO_VOTE</td></tr>
 *   <tr><td>동률(0 초과)</td><td>판사 탕이에게 위임</td><td>AI_JUDGMENT</td></tr>
 * </table>
 */
public final class VerdictDecider {

    private VerdictDecider() {
    }

    /**
     * 표 분포 하나로 판결을 정한다.
     *
     * <p>피고 혼자인 그룹({@code totalVoters == 0})은 표가 들어올 수 없어 0:0 으로 떨어진다 —
     * 별도 분기를 두지 않는다. 「아무도 유죄라고 하지 않았다」는 점에서 무투표와 같은 상황이다.
     *
     * @return 동률이면 {@link VerdictDecision#needsAi()} 가 참인 위임 표시
     */
    public static VerdictDecision decide(VerdictTallyRow row) {
        int guilty = row.getGuiltyCount();
        int innocent = row.getInnocentCount();

        if (guilty > innocent) {
            return VerdictDecision.guiltyByVote();
        }
        if (innocent > guilty) {
            return VerdictDecision.innocentByVote();
        }
        if (guilty == 0) {
            return VerdictDecision.innocentByNoVote();
        }
        return VerdictDecision.delegateToAi();
    }
}

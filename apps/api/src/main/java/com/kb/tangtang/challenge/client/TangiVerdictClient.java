package com.kb.tangtang.challenge.client;

import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;

/**
 * 판사 탕이 — 표가 동률일 때만 부르는 판결 위임 (이슈 #172).
 *
 * <p><b>인터페이스로 떼어 둔 이유</b>는 배치 단위 테스트에 스텁을 끼우기 위해서다. 개표 배치의
 * 핵심 검증은 「동률일 때만 부르는가 · 실패해도 재판이 끝나는가」이고, 그걸 확인하려고 매번
 * OpenAI 를 두드릴 수는 없다.
 *
 * <p><b>호출부는 트랜잭션 밖에서 부른다.</b> 안에서 부르면 HTTP 응답을 기다리는 동안 DB 커넥션과
 * 행 잠금을 잡고 있게 된다. ({@code GroupVerdictBatchService} 참고)
 */
public interface TangiVerdictClient {

    /**
     * 사건 한 건을 판사 탕이에게 넘겨 판결을 받는다.
     *
     * <p>호출 실패·타임아웃·응답 스키마 불일치는 <b>예외로 올린다.</b> 여기서 무죄로 삼켜 버리면
     * 호출부가 「탕이가 무죄라고 판단했다」와 「탕이를 못 불렀다」를 구분하지 못한다 —
     * 사용자에게 보일 사유 문구가 달라야 하는 두 상황이다. 대체 판결은 호출부가 정한다.
     *
     * @param row 표가 동률인 기소. 사건 개요·변론·표 분포가 모두 들어 있다
     * @return 결론이 난 판결({@link VerdictDecision#needsAi()} 이 거짓). 항상 {@code AI_JUDGMENT}
     * @throws RuntimeException 호출에 실패했거나 판결을 읽어 낼 수 없을 때
     */
    VerdictDecision judge(VerdictTallyRow row);
}

package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.client.TangiVerdictClient;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictMethod;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 개표 배치 (이슈 #172).
 *
 * <p>개표 규칙 자체는 {@link VerdictDeciderTest} 가 본다. 여기서 지키는 것은 <b>배치의 안전
 * 장치</b>다 — 동률일 때만 LLM 을 부르는가, LLM 이 죽어도 재판이 끝나는가, 한 건의 실패가
 * 나머지를 막지 않는가.
 */
@ExtendWith(MockitoExtension.class)
class GroupVerdictBatchServiceTest {

    private static final int DEFENSE_HOURS = 6;
    private static final int VOTE_HOURS = 24;

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private GroupVerdictTransitionService transitionService;
    @Mock private TangiVerdictClient tangiVerdictClient;

    private GroupVerdictBatchService service() {
        return new GroupVerdictBatchService(indictmentMapper, transitionService, tangiVerdictClient,
                DEFENSE_HOURS, VOTE_HOURS);
    }

    private static VerdictTallyRow tally(long indictmentId, int guilty, int innocent) {
        VerdictTallyRow row = new VerdictTallyRow();
        row.setIndictmentId(indictmentId);
        row.setGroupId(1L);
        row.setUserId(2L);
        row.setGuiltyCount(guilty);
        row.setInnocentCount(innocent);
        row.setTotalVoters(guilty + innocent);
        return row;
    }

    private void due(VerdictTallyRow... rows) {
        when(indictmentMapper.findVotingToTally(DEFENSE_HOURS, VOTE_HOURS)).thenReturn(List.of(rows));
    }

    /**
     * 마감 시각은 프로퍼티라 SQL 에 상수로 박을 수 없다({@code ${}} 금지). 서비스가 옛 값으로
     * 돌면 개표 시점이 화면에 표시된 마감과 어긋난다.
     */
    @Test
    @DisplayName("마감 시간은 프로퍼티 값을 그대로 SQL 에 넘긴다")
    void passesConfiguredHours() {
        when(indictmentMapper.findVotingToTally(DEFENSE_HOURS, VOTE_HOURS)).thenReturn(List.of());

        assertEquals(0, service().confirmDueVerdicts());

        verify(indictmentMapper).findVotingToTally(DEFENSE_HOURS, VOTE_HOURS);
        verifyNoInteractions(transitionService);
    }

    /**
     * <b>표로 결론이 나는 재판까지 LLM 을 부르면</b> 평시에도 매 틱 OpenAI 요금이 나가고
     * 배치 한 틱이 HTTP 대기로 길어진다.
     */
    @Test
    @DisplayName("표로 결론이 나면 판사 탕이를 부르지 않는다")
    void doesNotCallAiWhenVotesDecide() {
        due(tally(1L, 3, 1), tally(2L, 0, 2), tally(3L, 0, 0));
        when(transitionService.confirm(any(), any())).thenReturn(true);

        assertEquals(3, service().confirmDueVerdicts());

        verifyNoInteractions(tangiVerdictClient);
    }

    @Test
    @DisplayName("동률이면 판사 탕이의 판결을 그대로 확정한다")
    void tieUsesAiVerdict() {
        VerdictTallyRow tie = tally(1L, 2, 2);
        due(tie);
        when(tangiVerdictClient.judge(tie))
                .thenReturn(VerdictDecision.byAi(IndictmentStatus.GUILTY, "변론이 사실과 다르다"));
        when(transitionService.confirm(any(), any())).thenReturn(true);

        service().confirmDueVerdicts();

        VerdictDecision used = capturedDecision();
        assertEquals(IndictmentStatus.GUILTY, used.getStatus());
        assertEquals(VerdictMethod.AI_JUDGMENT, used.getMethod());
        assertEquals("변론이 사실과 다르다", used.getAiReason());
    }

    /**
     * <b>이 테스트가 「끝나지 않는 재판」을 막는다.</b> API 키 만료 같은 원인이면 재시도해도
     * 계속 실패하고, 예외를 그대로 올리면 이 재판은 다음 틱마다 다시 집혀 영원히 확정되지 않는다.
     * 무죄를 기본값으로 두는 것은 0표 무죄 추정({@code NO_VOTE})과 일관되기 때문이다.
     */
    @Test
    @DisplayName("판사 탕이 호출이 실패하면 무죄 추정으로 확정한다")
    void aiFailureFallsBackToInnocent() {
        VerdictTallyRow tie = tally(1L, 2, 2);
        due(tie);
        when(tangiVerdictClient.judge(tie)).thenThrow(new IllegalStateException("timeout"));
        when(transitionService.confirm(any(), any())).thenReturn(true);

        assertEquals(1, service().confirmDueVerdicts());

        VerdictDecision used = capturedDecision();
        assertEquals(IndictmentStatus.INNOCENT, used.getStatus());
        /* 사유가 있어야 하므로 method 는 VOTE 가 아니라 AI_JUDGMENT 여야 한다 (ck_ind_ai_reason) */
        assertEquals(VerdictMethod.AI_JUDGMENT, used.getMethod());
        assertEquals(GroupVerdictBatchService.AI_FAILURE_REASON, used.getAiReason());
    }

    /** 응답을 200 으로 받았는데 판결을 읽어 내지 못한 경우도 같은 대체 판결로 떨어져야 한다. */
    @Test
    @DisplayName("판사 탕이가 결론 없는 응답을 주면 같은 대체 판결로 확정한다")
    void aiUnresolvedResponseFallsBack() {
        VerdictTallyRow tie = tally(1L, 1, 1);
        due(tie);
        when(tangiVerdictClient.judge(tie)).thenReturn(VerdictDecision.delegateToAi());
        when(transitionService.confirm(any(), any())).thenReturn(true);

        service().confirmDueVerdicts();

        assertEquals(GroupVerdictBatchService.AI_FAILURE_REASON, capturedDecision().getAiReason());
    }

    /**
     * 트랜잭션이 기소 단위라 앞 건은 이미 커밋됐다. 여기서 예외가 위로 튀면 나머지 재판이
     * 이번 틱에 확정되지 않고, 그 사이 사용자는 「투표 완료」 화면 앞에 멈춰 있다.
     */
    @Test
    @DisplayName("한 재판의 실패가 나머지를 막지 않는다")
    void oneFailureDoesNotStopTheRest() {
        VerdictTallyRow broken = tally(1L, 3, 1);
        VerdictTallyRow fine = tally(2L, 1, 3);
        due(broken, fine);
        when(transitionService.confirm(any(), any()))
                .thenThrow(new IllegalStateException("DB 오류"))
                .thenReturn(true);

        assertEquals(1, service().confirmDueVerdicts());

        verify(transitionService, times(2)).confirm(any(), any());
    }

    /** 다른 인스턴스가 먼저 확정한 건은 세지 않는다. 로그의 「확정=n」이 실제 전이 수여야 한다. */
    @Test
    @DisplayName("이미 확정된 재판은 확정 건수에 세지 않는다")
    void doesNotCountAlreadyConfirmed() {
        due(tally(1L, 3, 1), tally(2L, 1, 3));
        when(transitionService.confirm(any(), any())).thenReturn(false, true);

        assertEquals(1, service().confirmDueVerdicts());
    }

    @Test
    @DisplayName("개표 대상이 없으면 확정 시도를 하지 않는다")
    void noopWhenNothingDue() {
        when(indictmentMapper.findVotingToTally(DEFENSE_HOURS, VOTE_HOURS)).thenReturn(List.of());

        service().confirmDueVerdicts();

        verify(transitionService, never()).confirm(any(), any());
    }

    private VerdictDecision capturedDecision() {
        ArgumentCaptor<VerdictDecision> captor = ArgumentCaptor.forClass(VerdictDecision.class);
        verify(transitionService).confirm(any(), captor.capture());
        return captor.getValue();
    }
}

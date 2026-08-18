package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictMethod;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 판결 확정 한 건과 그 뒤처리 (이슈 #172).
 *
 * <p>여기서 지키는 것은 셋이다 — <b>멱등</b>(이미 확정된 재판은 아무것도 하지 않는다),
 * <b>뒤처리 분리</b>(유죄만 목숨을 깎고 무죄만 감액한다), <b>이벤트 한 번</b>(채팅·알림이
 * 여기서 나가므로 두 번 발행하면 사용자에게 두 번 보인다).
 */
@ExtendWith(MockitoExtension.class)
class GroupVerdictTransitionServiceTest {

    private static final long INDICTMENT_ID = 77L;
    private static final long GROUP_ID = 5L;
    private static final long USER_ID = 9L;
    private static final long RESULT_ID = 31L;

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private ApplicationEventPublisher events;

    private GroupVerdictTransitionService service() {
        return new GroupVerdictTransitionService(indictmentMapper, groupMemberMapper, resultMapper, events);
    }

    private VerdictTallyRow row(BigDecimal deduction) {
        VerdictTallyRow row = new VerdictTallyRow();
        row.setIndictmentId(INDICTMENT_ID);
        row.setGroupId(GROUP_ID);
        row.setUserId(USER_ID);
        row.setResultId(RESULT_ID);
        row.setNickname("탕구리");
        row.setDeductionAmount(deduction);
        return row;
    }

    private void confirmReturns(int changed) {
        when(indictmentMapper.confirmVerdict(anyLong(), anyString(), anyInt(), anyString(), any()))
                .thenReturn(changed);
    }

    /**
     * <b>이 테스트가 이 파일에서 가장 중요하다.</b> 배치를 두 번 돌리거나 두 인스턴스가 같은 틱을
     * 잡으면 두 번째 {@code confirmVerdict} 가 0행을 바꾼다. 그 0 을 보지 않고 뒤처리로 넘어가면
     * <b>목숨이 두 번 깎이고</b> 감액이 두 번 더해지며 알림도 두 번 나간다.
     */
    @Test
    @DisplayName("이미 확정된 재판이면 목숨·감액·이벤트를 전부 건너뛴다")
    void skipsEverythingWhenAlreadyConfirmed() {
        confirmReturns(0);

        assertFalse(service().confirm(row(null), VerdictDecision.guiltyByVote()));

        verifyNoInteractions(groupMemberMapper);
        verifyNoInteractions(resultMapper);
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("유죄면 목숨을 깎고 감액은 건드리지 않는다")
    void guiltyDecreasesLifeOnly() {
        confirmReturns(1);
        when(groupMemberMapper.decreaseLife(GROUP_ID, USER_ID)).thenReturn(1);

        assertTrue(service().confirm(row(new BigDecimal("12000")), VerdictDecision.guiltyByVote()));

        verify(indictmentMapper).confirmVerdict(INDICTMENT_ID, IndictmentStatus.GUILTY.name(), 1,
                VerdictMethod.VOTE.name(), null);
        verify(groupMemberMapper).decreaseLife(GROUP_ID, USER_ID);
        /* 유죄인데 감액을 반영하면 벌은 받고 소비는 깎이는 이중 처리가 된다 */
        verify(resultMapper, never()).addVerdictDeduction(anyLong(), any());
    }

    /**
     * 이미 0목숨이면 {@code decreaseLife} 가 0행을 바꾼다. 오류가 아니다 — 판결 자체는 성립하고
     * 탈락 판정은 종료 후 확정 배치가 따로 내린다. 다만 문구는 갈라야 한다.
     */
    @Test
    @DisplayName("남은 목숨이 없어도 판결은 확정되고 문구만 달라진다")
    void guiltyWithoutLivesStillConfirms() {
        confirmReturns(1);
        when(groupMemberMapper.decreaseLife(GROUP_ID, USER_ID)).thenReturn(0);

        assertTrue(service().confirm(row(null), VerdictDecision.guiltyByVote()));

        assertTrue(publishedSummary().contains("남은 목숨이 없어요"));
    }

    @Test
    @DisplayName("무죄면 인정된 감액을 결산 행에 반영하고 목숨은 건드리지 않는다")
    void innocentAddsDeductionOnly() {
        confirmReturns(1);

        assertTrue(service().confirm(row(new BigDecimal("12000")), VerdictDecision.innocentByVote()));

        verify(indictmentMapper).confirmVerdict(INDICTMENT_ID, IndictmentStatus.INNOCENT.name(), 0,
                VerdictMethod.VOTE.name(), null);
        verify(resultMapper).addVerdictDeduction(RESULT_ID, new BigDecimal("12000"));
        verifyNoInteractions(groupMemberMapper);
    }

    /**
     * 변론 없이 마감돼 투표로 넘어간 재판은 {@code tbl_defense} 행 자체가 없어 감액이 NULL 이다.
     * 그대로 UPDATE 로 넘기면 {@code verdict_deduction_amount} 가 NULL 이 되어
     * {@code effective_amount}(생성 컬럼)까지 NULL 로 무너진다.
     */
    @Test
    @DisplayName("변론이 없으면 감액 UPDATE 를 아예 부르지 않는다")
    void innocentWithoutDefenseSkipsDeduction() {
        confirmReturns(1);

        assertTrue(service().confirm(row(null), VerdictDecision.innocentByNoVote()));

        verifyNoInteractions(resultMapper);
        verifyNoInteractions(groupMemberMapper);
    }

    /** 변론은 냈지만 대납·공동부담이 없어 감액이 0인 경우. 0을 더하는 UPDATE 는 흔적만 남긴다. */
    @Test
    @DisplayName("감액이 0이면 UPDATE 하지 않는다")
    void zeroDeductionSkipsUpdate() {
        confirmReturns(1);

        service().confirm(row(BigDecimal.ZERO), VerdictDecision.innocentByVote());

        verifyNoInteractions(resultMapper);
    }

    /**
     * {@code ai_verdict_reason} 은 {@code AI_JUDGMENT} 일 때만 채워야 한다({@code ck_ind_ai_reason}).
     * 사유가 UPDATE 까지 그대로 전달되지 않으면 화면에 판결 근거가 비어 보인다.
     */
    @Test
    @DisplayName("AI 판결은 사유를 그대로 UPDATE 에 실어 보낸다")
    void aiVerdictCarriesReason() {
        confirmReturns(1);

        service().confirm(row(null), VerdictDecision.byAi(IndictmentStatus.INNOCENT, "사정이 딱하다"));

        verify(indictmentMapper).confirmVerdict(INDICTMENT_ID, IndictmentStatus.INNOCENT.name(), 0,
                VerdictMethod.AI_JUDGMENT.name(), "사정이 딱하다");
    }

    /** 채팅 시스템 메시지와 GROUP_JUDGMENT 알림이 이 이벤트 하나에서 갈라져 나간다. */
    @Test
    @DisplayName("확정 이벤트를 한 번 발행한다")
    void publishesVerdictConfirmedOnce() {
        confirmReturns(1);
        when(groupMemberMapper.decreaseLife(GROUP_ID, USER_ID)).thenReturn(1);

        service().confirm(row(null), VerdictDecision.guiltyByVote());

        ArgumentCaptor<GroupTrialEvents.VerdictConfirmed> captor =
                ArgumentCaptor.forClass(GroupTrialEvents.VerdictConfirmed.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(GROUP_ID, captor.getValue().getGroupId());
        assertEquals(INDICTMENT_ID, captor.getValue().getIndictmentId());
        assertTrue(captor.getValue().getSummary().contains("탕구리"));
    }

    private String publishedSummary() {
        ArgumentCaptor<GroupTrialEvents.VerdictConfirmed> captor =
                ArgumentCaptor.forClass(GroupTrialEvents.VerdictConfirmed.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue().getSummary();
    }

    /**
     * AI 위임 상태({@code needsAi()})는 결론이 아니다. 배치가 LLM 호출을 빠뜨리고 넘기면
     * {@code status} 가 NULL 이라 UPDATE 가 CHECK 제약에 걸려 죽는다 — 그 전에 잡는다.
     */
    @Test
    @DisplayName("결론이 안 난 AI 위임 상태로는 확정하지 않는다")
    void rejectsUnresolvedAiDelegation() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().confirm(row(null), VerdictDecision.delegateToAi()));

        verify(indictmentMapper, never())
                .confirmVerdict(anyLong(), anyString(), anyInt(), anyString(), eq(null));
    }
}

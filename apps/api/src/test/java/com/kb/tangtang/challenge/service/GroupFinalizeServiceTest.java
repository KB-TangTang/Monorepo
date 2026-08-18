package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.FinalOutcome;
import com.kb.tangtang.challenge.domain.GroupChallengeEvents;
import com.kb.tangtang.challenge.domain.GroupFinalizeRow;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 그룹 챌린지 최종 결과 확정 (이슈 #172).
 *
 * <p>여기서 지키는 것은 세 가지다 — 평가 주기별 탈락 판정, 탈락자 공동 최하위,
 * 그리고 <b>상태 전이가 0행이면 이벤트를 발행하지 않는다</b>는 멱등 장치다.
 */
@ExtendWith(MockitoExtension.class)
class GroupFinalizeServiceTest {

    @Mock private ChallengeGroupMapper challengeGroupMapper;
    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private GroupFinalizeService service;

    private static ChallengeGroup group(EvalType evalType, int limitAmount) {
        return ChallengeGroup.builder()
                .id(1L)
                .evalType(evalType.name())
                .limitAmount(limitAmount)
                .endDate(LocalDate.of(2026, 8, 31))
                .build();
    }

    private static GroupFinalizeRow row(long userId, int livesCount, long chargeAmount) {
        GroupFinalizeRow row = new GroupFinalizeRow();
        row.setUserId(userId);
        row.setLivesCount(livesCount);
        row.setChargeAmount(BigDecimal.valueOf(chargeAmount));
        return row;
    }

    private void members(GroupFinalizeRow... rows) {
        when(resultMapper.findFinalConsumption(1L)).thenReturn(List.of(rows));
    }

    private void closes() {
        when(challengeGroupMapper.updateStatusIfCurrent(1L,
                ChallengeGroupStatus.JUDGING.name(), ChallengeGroupStatus.CLOSED.name()))
                .thenReturn(1);
    }

    /** userId → (판정, 등수, 부담금) */
    private Map<Long, Object[]> capturedWrites(int times) {
        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> outcome = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> rank = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(groupMemberMapper, org.mockito.Mockito.times(times)).finalizeMember(
                anyLong(), userId.capture(), outcome.capture(), rank.capture(), amount.capture());

        Map<Long, Object[]> written = new HashMap<>();
        for (int i = 0; i < userId.getAllValues().size(); i++) {
            written.put(userId.getAllValues().get(i), new Object[]{
                    outcome.getAllValues().get(i),
                    rank.getAllValues().get(i),
                    amount.getAllValues().get(i)});
        }
        return written;
    }

    /**
     * 일일평가는 목숨만 본다. 소비액으로 다시 판정하면 <b>무죄로 끝난 날까지 탈락 사유로
     * 되살아난다</b> — 하루하루의 초과는 이미 재판을 거쳐 목숨으로 환산됐다.
     */
    @Test
    @DisplayName("일일평가는 목숨이 0인 사람만 탈락한다 — 한도를 넘겨도 목숨이 남으면 완주")
    void dailyEliminatesByLivesOnly() {
        members(row(10L, 0, 30_000), row(20L, 2, 900_000));
        closes();

        assertTrue(service.finalizeGroup(group(EvalType.DAILY, 100_000)));

        Map<Long, Object[]> written = capturedWrites(2);
        assertEquals(FinalOutcome.ELIMINATED.name(), written.get(10L)[0]);
        assertEquals(FinalOutcome.SURVIVED.name(), written.get(20L)[0]);
    }

    /**
     * 기간평가는 목숨을 보지 않는다. {@code lives_count} 가 1로 고정이라 유죄 한 번이면 0이 되는데,
     * 무죄로 끝났거나 아직 재판이 없는 그룹까지 같은 기준으로 판정하려면 소비액을 봐야 한다.
     */
    @Test
    @DisplayName("기간평가는 한도 초과만 탈락한다 — 목숨이 0이어도 한도 이내면 완주")
    void periodEliminatesByAmountOnly() {
        members(row(10L, 0, 90_000), row(20L, 1, 100_001));
        closes();

        service.finalizeGroup(group(EvalType.PERIOD, 100_000));

        Map<Long, Object[]> written = capturedWrites(2);
        assertEquals(FinalOutcome.SURVIVED.name(), written.get(10L)[0]);
        assertEquals(FinalOutcome.ELIMINATED.name(), written.get(20L)[0]);
    }

    /** 경계는 「초과」다. 한도와 정확히 같으면 완주다 — 화면의 「한도 이내」 표시와 같은 기준. */
    @Test
    @DisplayName("기간평가 — 한도와 정확히 같으면 완주다")
    void periodExactLimitSurvives() {
        members(row(10L, 1, 100_000));
        closes();

        service.finalizeGroup(group(EvalType.PERIOD, 100_000));

        assertEquals(FinalOutcome.SURVIVED.name(), capturedWrites(1).get(10L)[0]);
    }

    /**
     * 적게 쓴 사람이 1위다. 같은 금액이면 공동 순위고 뒤 사람은 그만큼 건너뛴다(1·1·3).
     * 금액이 같은데 {@code user_id} 로 앞뒤를 가르면 화면상 근거 없는 차이가 생긴다.
     */
    @Test
    @DisplayName("생존자는 소비액 오름차순 — 같은 금액이면 공동 순위")
    void survivorsRankedByAmountAscendingWithTies() {
        members(row(10L, 1, 50_000), row(20L, 1, 10_000), row(30L, 1, 10_000));
        closes();

        service.finalizeGroup(group(EvalType.DAILY, 100_000));

        Map<Long, Object[]> written = capturedWrites(3);
        assertEquals(1, written.get(20L)[1]);
        assertEquals(1, written.get(30L)[1]);
        assertEquals(3, written.get(10L)[1]);
    }

    /**
     * 탈락자끼리 소비액으로 순위를 가르지 않는다. 탈락은 「완주 실패」 하나로 같은 결과고,
     * 덜 쓴 탈락자가 더 나은 등수를 받으면 완주의 의미가 흐려진다.
     */
    @Test
    @DisplayName("탈락자는 소비액과 무관하게 전원 공동 최하위")
    void eliminatedShareLastRank() {
        members(row(10L, 1, 80_000), row(20L, 1, 20_000), row(30L, 0, 1_000), row(40L, 0, 999_000));
        closes();

        service.finalizeGroup(group(EvalType.DAILY, 100_000));

        Map<Long, Object[]> written = capturedWrites(4);
        assertEquals(1, written.get(20L)[1]);
        assertEquals(2, written.get(10L)[1]);
        assertEquals(3, written.get(30L)[1]);
        assertEquals(3, written.get(40L)[1]);
    }

    /** 부담금은 판정용 소비액과 같은 값이다. 다른 값을 쓰면 화면의 순위와 금액이 어긋난다. */
    @Test
    @DisplayName("final_charge_amount 는 조회한 실효 소비액 그대로다")
    void writesChargeAmountAsQueried() {
        members(row(10L, 1, 73_500));
        closes();

        service.finalizeGroup(group(EvalType.DAILY, 100_000));

        assertEquals(0, BigDecimal.valueOf(73_500)
                .compareTo((BigDecimal) capturedWrites(1).get(10L)[2]));
    }

    /**
     * <b>이 테스트가 중복 알림·중복 전적 갱신을 막는다.</b> 배치를 두 번 돌리거나 인스턴스가
     * 둘이면 같은 그룹이 두 번 잡히는데, 상태 전이가 0행이면 이미 다른 쪽이 확정한 것이다.
     */
    @Test
    @DisplayName("상태 전이가 0행이면 이벤트를 발행하지 않는다")
    void doesNotPublishWhenAlreadyClosed() {
        members(row(10L, 1, 10_000));
        when(challengeGroupMapper.updateStatusIfCurrent(anyLong(), anyString(), anyString()))
                .thenReturn(0);

        assertFalse(service.finalizeGroup(group(EvalType.DAILY, 100_000)));

        verify(events, never()).publishEvent(any(Object.class));
    }

    /**
     * {@code endDate} 는 수신 시각이 아니라 챌린지가 끝난 달을 가리켜야 한다. 말일에 끝난 그룹은
     * 확정이 다음 달로 넘어가는 것이 정상 경로라, 수신 시각으로 월을 뽑으면 전적이 엉뚱한 달에 붙는다.
     */
    @Test
    @DisplayName("확정 이벤트에 종료일과 참여자 전원이 실린다 — 탈락자 포함")
    void publishesClosedEventWithEndDateAndAllMembers() {
        members(row(10L, 1, 10_000), row(20L, 0, 50_000));
        closes();

        service.finalizeGroup(group(EvalType.DAILY, 100_000));

        ArgumentCaptor<GroupChallengeEvents.GroupChallengeClosed> captor =
                ArgumentCaptor.forClass(GroupChallengeEvents.GroupChallengeClosed.class);
        verify(events).publishEvent(captor.capture());

        GroupChallengeEvents.GroupChallengeClosed published = captor.getValue();
        assertEquals(1L, published.getGroupId());
        assertEquals(LocalDate.of(2026, 8, 31), published.getEndDate());
        assertEquals(List.of(10L, 20L), published.getMemberUserIds());
    }

    /**
     * 참여자가 하나도 없는 그룹(모집 미달 정리와 엇갈린 예외 경로)도 상태만은 끝나야 한다.
     * {@code JUDGING} 에 남으면 이 그룹은 매 틱 다시 조회된다.
     */
    @Test
    @DisplayName("참여자가 없어도 상태는 CLOSED 로 끝낸다")
    void closesEvenWithoutMembers() {
        when(resultMapper.findFinalConsumption(1L)).thenReturn(List.of());
        closes();

        assertTrue(service.finalizeGroup(group(EvalType.DAILY, 100_000)));

        verify(groupMemberMapper, never())
                .finalizeMember(anyLong(), anyLong(), anyString(), anyInt(), any());
    }
}

package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.IndictmentTarget;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 그룹 하나의 평가·기소 (이슈 #168).
 *
 * <p>검증의 무게중심은 <b>어느 날짜를 집계하는가</b>다. 이 배치의 존재 이유가 심야 거래를
 * 다음 날 배치가 주워 담는 것이라, 날짜 선택이 틀리면 기능 자체가 없는 것과 같다.
 *
 * <p>발행된 이벤트는 리스트에 모아 본다.
 * ({@link ChallengeGroupStatusTransitionServiceTest} 와 같은 방식)
 */
@ExtendWith(MockitoExtension.class)
class GroupChallengeEvaluationServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long USER_ID = 11L;
    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 16);

    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private IndictmentMapper indictmentMapper;

    private final List<NotificationRequestedEvent> published = new ArrayList<>();
    private GroupChallengeEvaluationService service;

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher publisher =
                event -> published.add((NotificationRequestedEvent) event);
        service = new GroupChallengeEvaluationService(resultMapper, indictmentMapper, publisher);
    }

    @Test
    @DisplayName("매 틱 어제·오늘 두 날짜를 다시 집계한다 — 심야 거래가 어제 집계에 반영되도록")
    void aggregatesYesterdayAndToday() {
        LocalDate today = LocalDate.of(2026, 8, 15);

        service.evaluate(dailyGroup(), today);

        verify(resultMapper).upsertDailyResults(GROUP_ID, LocalDate.of(2026, 8, 14));
        verify(resultMapper).upsertDailyResults(GROUP_ID, today);
    }

    @Test
    @DisplayName("챌린지 시작일에는 오늘만 집계한다 — 기간 밖 날짜에 행을 만들지 않는다")
    void skipsDatesOutsideThePeriod() {
        service.evaluate(dailyGroup(), START);

        verify(resultMapper).upsertDailyResults(GROUP_ID, START);
        verify(resultMapper, never()).upsertDailyResults(GROUP_ID, START.minusDays(1));
    }

    @Test
    @DisplayName("종료 다음 날에는 마지막 날만 집계한다 — 그 날 심야 거래를 여기서 주워 담는다")
    void aggregatesLastDayOnTheDayAfter() {
        service.evaluate(dailyGroup(), END.plusDays(1));

        verify(resultMapper).upsertDailyResults(GROUP_ID, END);
        verify(resultMapper, never()).upsertDailyResults(GROUP_ID, END.plusDays(1));
    }

    @Test
    @DisplayName("한도를 넘기면 DEFENSE_WAIT 기소가 생기고 피고에게 변론 화면 링크가 간다")
    void indictsAndNotifiesTheAccused() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        /* 어제도 같이 본다. 어제 행이 이번 틱에 처음 한도를 넘겼을 수 있어서다 */
        when(resultMapper.findOverLimitDaily(GROUP_ID, today.minusDays(1))).thenReturn(List.of());
        when(resultMapper.findOverLimitDaily(GROUP_ID, today))
                .thenReturn(List.of(target(today, 36_700)));
        when(indictmentMapper.insertIndictment(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, com.kb.tangtang.challenge.domain.Indictment.class).setId(99L);
            return 1;
        });

        assertEquals(1, service.evaluate(dailyGroup(), today));

        verify(indictmentMapper).insertIndictment(org.mockito.ArgumentMatchers.argThat(indictment ->
                IndictmentStatus.DEFENSE_WAIT.name().equals(indictment.getStatus())
                        && indictment.getResultId() == 500L
                        && indictment.getUserId() == USER_ID
                        && indictment.getResult() == null
                        && indictment.getVerdictMethod() == null));

        assertEquals(1, published.size());
        NotificationRequestedEvent event = published.get(0);
        assertEquals(USER_ID, event.userId());
        assertEquals(NotificationType.GROUP_TRIAL_OPENED, event.type());
        assertEquals("8월 15일", event.params().get("period"));
        assertEquals("36,700원", event.params().get("amount"));
        assertEquals("/group-challenges/7/defense/99", event.deepLinkUrl(),
                "피고가 받는 알림이라 재판 상세가 아니라 변론 첫 화면으로 보낸다");
    }

    @Test
    @DisplayName("다른 인스턴스가 먼저 기소했으면(UNIQUE 충돌) 알림을 보내지 않는다")
    void skipsNotificationOnDuplicateIndictment() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        when(resultMapper.findOverLimitDaily(GROUP_ID, today.minusDays(1))).thenReturn(List.of());
        when(resultMapper.findOverLimitDaily(GROUP_ID, today))
                .thenReturn(List.of(target(today, 36_700)));
        when(indictmentMapper.insertIndictment(any()))
                .thenThrow(new DuplicateKeyException("uk_ind_result"));

        assertEquals(0, service.evaluate(dailyGroup(), today));

        assertTrue(published.isEmpty(), "기소를 만들지 못했으면 알림도 없어야 한다");
    }

    @Test
    @DisplayName("기간평가는 챌린지 중에는 기소하지 않는다 — 집계만 계속 갱신한다")
    void periodDoesNotIndictWhileRunning() {
        LocalDate today = LocalDate.of(2026, 8, 15);

        assertEquals(0, service.evaluate(periodGroup(), today));

        verify(resultMapper).upsertDailyResults(GROUP_ID, today);
        verify(resultMapper, never()).findOverLimitPeriod(anyLong());
        verify(resultMapper, never()).findOverLimitDaily(anyLong(), any());
    }

    @Test
    @DisplayName("기간평가는 종료 다음 날 누적액으로 한 번 기소한다. 기간이 문구에 들어간다")
    void periodIndictsAfterTheChallengeEnds() {
        when(resultMapper.findOverLimitPeriod(GROUP_ID))
                .thenReturn(List.of(target(END, 85_000)));
        when(indictmentMapper.insertIndictment(any())).thenReturn(1);

        assertEquals(1, service.evaluate(periodGroup(), END.plusDays(1)));

        assertEquals("8월 10일~8월 16일", published.get(0).params().get("period"));
        assertEquals("85,000원", published.get(0).params().get("amount"));
    }

    private ChallengeGroup dailyGroup() {
        return group("DAILY", 10_000);
    }

    private ChallengeGroup periodGroup() {
        return group("PERIOD", 70_000);
    }

    private ChallengeGroup group(String evalType, int limitAmount) {
        return ChallengeGroup.builder()
                .id(GROUP_ID)
                .groupName("커피값 줄이기")
                .categoryName("카페")
                .limitAmount(limitAmount)
                .evalType(evalType)
                .startDate(START)
                .endDate(END)
                .status("ACTIVE")
                .build();
    }

    private IndictmentTarget target(LocalDate challengeDate, int amount) {
        return IndictmentTarget.builder()
                .resultId(500L)
                .userId(USER_ID)
                .challengeDate(challengeDate)
                .amount(BigDecimal.valueOf(amount))
                .build();
    }
}

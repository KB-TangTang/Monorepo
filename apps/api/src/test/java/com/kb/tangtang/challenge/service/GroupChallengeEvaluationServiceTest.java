package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
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
 *
 * <p>한 기소가 <b>세 종류</b>를 쏘므로(알림 · 채팅 적발 · 채팅 개시) 리스트는 {@code Object} 로
 * 받고 {@link #eventsOf} 로 걸러 쓴다. 캐스팅해 담으면 새 이벤트가 늘 때마다 여기서 죽는다.
 */
@ExtendWith(MockitoExtension.class)
class GroupChallengeEvaluationServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long USER_ID = 11L;
    private static final String NICKNAME = "절약왕";
    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 16);

    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private IndictmentMapper indictmentMapper;

    private final List<Object> published = new ArrayList<>();
    private GroupChallengeEvaluationService service;

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher publisher = published::add;
        service = new GroupChallengeEvaluationService(resultMapper, indictmentMapper, publisher);
    }

    private <T> List<T> eventsOf(Class<T> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private NotificationRequestedEvent notification() {
        return eventsOf(NotificationRequestedEvent.class).get(0);
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

        assertEquals(1, eventsOf(NotificationRequestedEvent.class).size());
        NotificationRequestedEvent event = notification();
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

        assertTrue(published.isEmpty(), "기소를 만들지 못했으면 알림도 채팅 메시지도 없어야 한다");
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
        /* insertIndictment 는 useGeneratedKeys 라 id 를 채워 돌아온다. 딥링크·채팅 이벤트가 그 값을 쓴다 */
        when(indictmentMapper.insertIndictment(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, com.kb.tangtang.challenge.domain.Indictment.class).setId(99L);
            return 1;
        });

        assertEquals(1, service.evaluate(periodGroup(), END.plusDays(1)));

        assertEquals("8월 10일~8월 16일", notification().params().get("period"));
        assertEquals("85,000원", notification().params().get("amount"));
    }

    /* ══ 그룹 채팅 시스템 메시지 (이슈 #355) ═══════════════════════════════ */

    /**
     * 카드가 「적발 → 개시」 두 장으로 설계돼 있어(ChatSystemType) 둘 다 쏜다. 하나만 쏘면
     * 나머지 카드가 영영 안 쓰이는 죽은 코드가 된다.
     *
     * <p>순서까지 보는 이유는 표시 순서가 발행 순서에 매여 있기 때문이다 —
     * 메시지 번호를 Redis INCR 로 그 자리에서 발급하는 구조라 뒤집히면 화면에서 그대로 뒤집힌다.
     */
    @Test
    @DisplayName("기소가 생기면 채팅에 적발·개시 두 이벤트가 그 순서로 나간다")
    void indictmentPostsViolationThenTrialOpened() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        givenOneOverLimitTarget(today);

        service.evaluate(dailyGroup(), today);

        List<Object> chat = published.stream()
                .filter(e -> !(e instanceof NotificationRequestedEvent))
                .toList();
        assertEquals(2, chat.size(), "적발·개시 두 장이다");
        assertTrue(chat.get(0) instanceof GroupTrialEvents.ViolationDetected,
                "적발이 먼저다 — 순서가 뒤집히면 「변론이 시작됩니다」가 위에 뜬다");
        assertTrue(chat.get(1) instanceof GroupTrialEvents.TrialOpened);
    }

    @Test
    @DisplayName("채팅 이벤트는 그룹·기소 id 와 조회에서 받은 닉네임을 그대로 싣는다")
    void chatEventsCarryGroupIndictmentAndNickname() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        givenOneOverLimitTarget(today);

        service.evaluate(dailyGroup(), today);

        GroupTrialEvents.ViolationDetected violation =
                eventsOf(GroupTrialEvents.ViolationDetected.class).get(0);
        assertEquals(GROUP_ID, violation.getGroupId());
        assertEquals(99L, violation.getIndictmentId(), "INSERT 직후 채워진 기소 id 다");
        assertEquals(NICKNAME, violation.getTargetNickname());

        GroupTrialEvents.TrialOpened opened = eventsOf(GroupTrialEvents.TrialOpened.class).get(0);
        assertEquals(GROUP_ID, opened.getGroupId());
        assertEquals(99L, opened.getIndictmentId());
        assertEquals(NICKNAME, opened.getTargetNickname());
    }

    @Test
    @DisplayName("기소 대상이 없으면 채팅 이벤트도 없다")
    void noTargetsPostNothing() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        when(resultMapper.findOverLimitDaily(anyLong(), any())).thenReturn(List.of());

        assertEquals(0, service.evaluate(dailyGroup(), today));

        assertTrue(published.isEmpty(), "집계만 돈 틱에는 채팅방이 조용해야 한다");
    }

    /**
     * 멱등성은 {@code DuplicateKeyException} 가드에 얹혀 있다. 발행이 그 뒤에 있어야
     * 배치를 다시 돌려도 같은 기소로 카드가 두 번 뜨지 않는다.
     */
    @Test
    @DisplayName("이미 기소된 행이면 채팅 이벤트도 발행하지 않는다 — 배치 재실행에 멱등")
    void duplicateIndictmentPostsNoChatEvent() {
        LocalDate today = LocalDate.of(2026, 8, 15);
        when(resultMapper.findOverLimitDaily(GROUP_ID, today.minusDays(1))).thenReturn(List.of());
        when(resultMapper.findOverLimitDaily(GROUP_ID, today))
                .thenReturn(List.of(target(today, 36_700)));
        when(indictmentMapper.insertIndictment(any()))
                .thenThrow(new DuplicateKeyException("uk_ind_result"));

        service.evaluate(dailyGroup(), today);

        assertTrue(eventsOf(GroupTrialEvents.ViolationDetected.class).isEmpty());
        assertTrue(eventsOf(GroupTrialEvents.TrialOpened.class).isEmpty());
    }

    /** 한도 초과 대상 1명 + INSERT 가 기소 id 99 를 채우는 상황. */
    private void givenOneOverLimitTarget(LocalDate today) {
        when(resultMapper.findOverLimitDaily(GROUP_ID, today.minusDays(1))).thenReturn(List.of());
        when(resultMapper.findOverLimitDaily(GROUP_ID, today))
                .thenReturn(List.of(target(today, 36_700)));
        when(indictmentMapper.insertIndictment(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, com.kb.tangtang.challenge.domain.Indictment.class).setId(99L);
            return 1;
        });
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
                .nickname(NICKNAME)
                .build();
    }
}

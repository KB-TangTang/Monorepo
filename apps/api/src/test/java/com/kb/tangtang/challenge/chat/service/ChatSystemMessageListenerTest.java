package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatSystemMessageSpec;
import com.kb.tangtang.challenge.chat.domain.ChatSystemType;
import com.kb.tangtang.challenge.chat.domain.ChatVerdictInfo;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSystemMessageListenerTest {

    private static final String TRIAL_DEEP_LINK = "/group-challenges/7/trial/55";
    /* 사건번호에 연도가 들어가므로 시계를 고정한다. 안 그러면 해가 바뀌는 순간 테스트가 깨진다 */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-16T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private ChatMessageService chatMessageService;

    private ChatSystemMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChatSystemMessageListener(chatMessageService, FIXED_CLOCK);
    }

    @Test
    @DisplayName("소비 위반 적발은 적발 카드 종류로 남기고 GROUP_TRIAL_OPENED 로 알린다")
    void violationDetectedUsesTrialType() {
        listener.onViolationDetected(new GroupTrialEvents.ViolationDetected(7L, 55L, "절약왕"));

        verify(chatMessageService).postSystemMessage(eq(7L), eq(new ChatSystemMessageSpec(
                "절약왕님의 소비가 한도를 넘었어요.", ChatSystemType.VIOLATION_DETECTED,
                TRIAL_DEEP_LINK, "2026-재판-0055", NotificationType.GROUP_TRIAL_OPENED)));
    }

    @Test
    @DisplayName("재판 개시는 개시 카드 종류로 남기고 GROUP_TRIAL_OPENED 로 알린다")
    void trialOpenedUsesTrialType() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 55L, "절약왕"));

        verify(chatMessageService).postSystemMessage(eq(7L), eq(new ChatSystemMessageSpec(
                "절약왕님이 피고인이에요. 변론이 시작됩니다.", ChatSystemType.TRIAL_OPENED,
                TRIAL_DEEP_LINK, "2026-재판-0055", NotificationType.GROUP_TRIAL_OPENED)));
    }

    @Test
    @DisplayName("변론 등록은 변론 카드 종류로 남기고 GROUP_DEFENSE_REGISTERED 를 쓴다")
    void defenseUsesNewType() {
        listener.onDefenseRegistered(new GroupTrialEvents.DefenseRegistered(7L, 55L, "절약왕"));

        verify(chatMessageService).postSystemMessage(eq(7L), eq(new ChatSystemMessageSpec(
                "절약왕님이 변론을 냈어요.", ChatSystemType.DEFENSE_REGISTERED,
                TRIAL_DEEP_LINK, "2026-재판-0055", NotificationType.GROUP_DEFENSE_REGISTERED)));
    }

    @Test
    @DisplayName("판결 확정은 판결 카드 종류로 남기고 GROUP_JUDGMENT 로 알린다")
    void verdictUsesJudgmentType() {
        listener.onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed.byVote(
                7L, 55L, "3만원 감액", false, 2, 4, 0));

        verify(chatMessageService).postSystemMessage(eq(7L), eq(new ChatSystemMessageSpec(
                "3만원 감액", ChatSystemType.VERDICT_CONFIRMED,
                TRIAL_DEEP_LINK, "2026-재판-0055", NotificationType.GROUP_JUDGMENT,
                ChatVerdictInfo.byVote(false, 2, 4, 0))));
    }

    @Test
    @DisplayName("유죄 판결은 표 분포와 차감된 목숨을 함께 싣는다 — 화면이 문구를 파싱하지 않게")
    void guiltyVerdictCarriesVotesAndLives() {
        listener.onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed.byVote(
                7L, 55L, "절약왕님 재판 — 유죄. 목숨 1개가 차감됐어요.", true, 4, 2, 1));

        ChatVerdictInfo verdict = captureSpec().verdict();
        assertEquals(ChatVerdictInfo.Outcome.GUILTY, verdict.outcome());
        assertEquals(4, verdict.guiltyVotes());
        assertEquals(2, verdict.innocentVotes());
        assertEquals(1, verdict.livesLost());
    }

    @Test
    @DisplayName("남은 목숨이 없어 못 깎은 유죄는 livesLost 가 0 이다 — 화면이 「1 차감」을 적으면 거짓말이 된다")
    void guiltyWithoutRemainingLifeCarriesZero() {
        listener.onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed.byVote(
                7L, 55L, "절약왕님 재판 — 유죄. 남은 목숨이 없어요.", true, 3, 1, 0));

        ChatVerdictInfo verdict = captureSpec().verdict();
        assertEquals(ChatVerdictInfo.Outcome.GUILTY, verdict.outcome());
        assertEquals(0, verdict.livesLost());
    }

    @Test
    @DisplayName("혐의 인정은 표 분포를 0:0 이 아니라 비운 채 싣는다 — 0:0 은 「아무도 안 던졌다」와 구별되지 않는다")
    void confessionCarriesNoVotes() {
        listener.onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed.byConfession(
                7L, 55L, "절약왕님이 혐의를 인정했어요. 유죄로 확정됩니다.", 1));

        ChatVerdictInfo verdict = captureSpec().verdict();
        assertEquals(ChatVerdictInfo.Outcome.GUILTY, verdict.outcome());
        assertNull(verdict.guiltyVotes());
        assertNull(verdict.innocentVotes());
        assertEquals(1, verdict.livesLost());
    }

    @Test
    @DisplayName("판결이 아닌 시스템 메시지에는 결과 값이 없다")
    void nonVerdictMessagesCarryNoVerdict() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 55L, "절약왕"));

        assertNull(captureSpec().verdict());
    }

    private ChatSystemMessageSpec captureSpec() {
        ArgumentCaptor<ChatSystemMessageSpec> spec = ArgumentCaptor.forClass(ChatSystemMessageSpec.class);
        verify(chatMessageService).postSystemMessage(anyLong(), spec.capture());
        return spec.getValue();
    }

    @Test
    @DisplayName("딥링크는 app:// 가 아니라 라우터 경로다")
    void deepLinkIsRouterPath() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 55L, "절약왕"));

        ArgumentCaptor<ChatSystemMessageSpec> spec = ArgumentCaptor.forClass(ChatSystemMessageSpec.class);
        verify(chatMessageService).postSystemMessage(anyLong(), spec.capture());
        assertTrue(spec.getValue().deepLink().startsWith("/"));
        assertEquals("/group-challenges/7/trial/55", spec.getValue().deepLink());
    }

    @Test
    @DisplayName("사건번호는 연도와 기소 id 를 조합한 표시용 문자열이다")
    void caseNumberCombinesYearAndIndictmentId() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 3L, "절약왕"));

        ArgumentCaptor<ChatSystemMessageSpec> spec = ArgumentCaptor.forClass(ChatSystemMessageSpec.class);
        verify(chatMessageService).postSystemMessage(anyLong(), spec.capture());
        assertEquals("2026-재판-0003", spec.getValue().caseNo());
    }
}

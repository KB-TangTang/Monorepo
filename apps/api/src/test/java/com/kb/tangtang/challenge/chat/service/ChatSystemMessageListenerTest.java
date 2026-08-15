package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.notification.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSystemMessageListenerTest {

    @Mock private ChatMessageService chatMessageService;

    @InjectMocks private ChatSystemMessageListener listener;

    @Test
    @DisplayName("재판 개시는 GROUP_TRIAL_OPENED 로 알린다")
    void trialOpenedUsesTrialType() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 55L, "절약왕"));

        verify(chatMessageService).postSystemMessage(eq(7L), anyString(), anyString(),
                eq(NotificationType.GROUP_TRIAL_OPENED));
    }

    @Test
    @DisplayName("변론 등록은 새로 추가한 GROUP_DEFENSE_REGISTERED 를 쓴다")
    void defenseUsesNewType() {
        listener.onDefenseRegistered(new GroupTrialEvents.DefenseRegistered(7L, 55L, "절약왕"));

        verify(chatMessageService).postSystemMessage(eq(7L), anyString(), anyString(),
                eq(NotificationType.GROUP_DEFENSE_REGISTERED));
    }

    @Test
    @DisplayName("판결 확정은 GROUP_JUDGMENT 로 알린다")
    void verdictUsesJudgmentType() {
        listener.onVerdictConfirmed(new GroupTrialEvents.VerdictConfirmed(7L, 55L, "3만원 감액"));

        verify(chatMessageService).postSystemMessage(eq(7L), anyString(), anyString(),
                eq(NotificationType.GROUP_JUDGMENT));
    }

    @Test
    @DisplayName("딥링크는 app:// 가 아니라 라우터 경로다")
    void deepLinkIsRouterPath() {
        listener.onTrialOpened(new GroupTrialEvents.TrialOpened(7L, 55L, "절약왕"));

        ArgumentCaptor<String> deepLink = ArgumentCaptor.forClass(String.class);
        verify(chatMessageService).postSystemMessage(anyLong(), anyString(), deepLink.capture(), eq(NotificationType.GROUP_TRIAL_OPENED));
        assertTrue(deepLink.getValue().startsWith("/"));
        assertEquals("/challenge/group/7/trial/55", deepLink.getValue());
    }
}

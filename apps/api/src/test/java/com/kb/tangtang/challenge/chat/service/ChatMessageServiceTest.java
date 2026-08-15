package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.service.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long SENDER_ID = 3L;

    @Mock private ChatMessageStore store;
    @Mock private ChatRoomAccessService access;
    @Mock private ChatSessionRegistry sessions;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationSender notificationSender;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private ChatMessageService service;

    @BeforeEach
    void setUp() {
        lenient().when(store.append(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(new ChatMessage(1L, ChatMessageType.TEXT, SENDER_ID, "절약왕",
                        "안녕", LocalDateTime.now()));
        lenient().when(access.memberIdsOf(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L, 12L));
        lenient().when(store.tryAcquireNotifyCooldown(anyLong(), anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("보낸 메시지를 구독 경로로 브로드캐스트한다")
    void broadcastsToSubscribers() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(messagingTemplate).convertAndSend(eq("/sub/chat/7"), any(Object.class));
    }

    @Test
    @DisplayName("본인과 접속 중인 사람의 안 읽은 수는 올리지 않는다")
    void skipsUnreadForSenderAndActiveUsers() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(store).increaseUnread(GROUP_ID, List.of(12L));
    }

    @Test
    @DisplayName("방 밖에 있는 사람에게만 전달 전용 알림을 보낸다")
    void pushesOnlyToUsersOutsideRoom() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(notificationSender).push(eq(12L), eq("chat"), any());
        verify(notificationSender, never()).push(eq(9L), anyString(), any());
        verify(notificationSender, never()).push(eq(SENDER_ID), anyString(), any());
    }

    @Test
    @DisplayName("쿨다운에 걸리면 알림을 건너뛰고 배지만 올린다")
    void skipsNotificationWhileCoolingDown() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID));
        when(store.tryAcquireNotifyCooldown(GROUP_ID, 12L)).thenReturn(false);

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(store).increaseUnread(eq(GROUP_ID), any());
        verify(notificationSender, never()).push(eq(12L), anyString(), any());
    }

    @Test
    @DisplayName("발송 전에 권한 검증이 저장보다 먼저 실행된다")
    void verifiesAccessBeforeSending() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of());

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(access).verifyCanEnter(GROUP_ID, SENDER_ID);
        InOrder order = inOrder(access, store);
        order.verify(access).verifyCanEnter(GROUP_ID, SENDER_ID);
        order.verify(store).append(eq(GROUP_ID), eq(ChatMessageType.TEXT), eq(SENDER_ID), anyString(), anyString());
    }

    @Test
    @DisplayName("시스템 메시지도 회원 조회가 저장보다 먼저 실행된다")
    void verifiesMemberLookupBeforeAppendForSystemMessage() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of());

        service.postSystemMessage(GROUP_ID, "재판이 열렸어요", "/challenge/group/7/trial",
                NotificationType.GROUP_TRIAL_OPENED);

        InOrder order = inOrder(access, store);
        order.verify(access).memberIdsOf(GROUP_ID);
        order.verify(store).append(eq(GROUP_ID), eq(ChatMessageType.SYSTEM), any(), any(), anyString());
    }

    @Test
    @DisplayName("시스템 메시지도 접속 중인 사람의 배지·알림은 건너뛴다")
    void skipsUnreadAndNotificationForActiveUsersOnSystemMessage() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(9L));

        service.postSystemMessage(GROUP_ID, "재판이 열렸어요", "/challenge/group/7/trial",
                NotificationType.GROUP_TRIAL_OPENED);

        verify(store).increaseUnread(GROUP_ID, List.of(SENDER_ID, 12L));
        verify(events, never()).publishEvent(new NotificationRequestedEvent(
                9L, NotificationType.GROUP_TRIAL_OPENED, Map.of("content", "재판이 열렸어요"),
                "/challenge/group/7/trial"));
    }

    @Test
    @DisplayName("멤버가 없는 방에는 시스템 메시지를 저장하지 않는다")
    void skipsSystemMessageWhenNoMembers() {
        when(access.memberIdsOf(GROUP_ID)).thenReturn(Set.of());

        service.postSystemMessage(GROUP_ID, "재판이 열렸어요", "/challenge/group/7/trial",
                NotificationType.GROUP_TRIAL_OPENED);

        verify(store, never()).append(anyLong(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("닉네임이 없어도 알림 루프가 끝까지 돈다")
    void continuesNotifyingWhenSenderNicknameIsNull() {
        when(store.append(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(new ChatMessage(1L, ChatMessageType.TEXT, SENDER_ID, null,
                        "안녕", LocalDateTime.now()));
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L));

        service.send(GROUP_ID, SENDER_ID, null, "안녕");

        verify(notificationSender).push(eq(12L), eq("chat"), any());
    }

    @Test
    @DisplayName("한 수신자 알림 실패가 나머지 수신자를 막지 않는다")
    void continuesNotifyingOtherRecipientsWhenOneFails() {
        when(access.memberIdsOf(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L, 12L, 15L));
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID));
        when(store.tryAcquireNotifyCooldown(GROUP_ID, 9L)).thenThrow(new RuntimeException("Redis 장애"));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(notificationSender).push(eq(12L), eq("chat"), any());
        verify(notificationSender).push(eq(15L), eq("chat"), any());
    }
}

package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.domain.ChatSystemMessageSpec;
import com.kb.tangtang.challenge.chat.domain.ChatSystemType;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.service.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long SENDER_ID = 3L;

    @Mock private ChatMessageStore store;
    @Mock private ChatRoomAccessService access;
    @Mock private ChatSessionRegistry sessions;
    @Mock private ChatBroadcaster broadcaster;
    @Mock private NotificationSender notificationSender;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private ChatMessageService service;

    @BeforeEach
    void setUp() {
        lenient().when(store.append(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(ChatMessage.of(1L, ChatMessageType.TEXT, SENDER_ID, "절약왕",
                        "안녕", LocalDateTime.now()));
        /* 시스템 메시지는 시스템 필드까지 받는 9인자 오버로드를 탄다 — 목이라 5인자 스텁이 대신 응답하지 않는다 */
        lenient().when(store.append(anyLong(), any(), any(), any(), anyString(), any(), any(), any(), any()))
                .thenReturn(new ChatMessage(2L, ChatMessageType.SYSTEM, null, null,
                        "재판이 열렸어요", LocalDateTime.now(), ChatSystemType.TRIAL_OPENED,
                        "/group-challenges/7/trial", "2026-재판-0001", null));
        lenient().when(access.memberIdsOf(GROUP_ID)).thenReturn(Set.of(SENDER_ID, 9L, 12L));
    }

    @Test
    @DisplayName("보낸 메시지를 구독 경로로 브로드캐스트한다")
    void broadcastsToSubscribers() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(broadcaster).broadcast(eq(GROUP_ID), any(ChatMessageDto.class));
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
    @DisplayName("채팅 알림의 딥링크는 라우터 경로와 같다 — 틀리면 눌러도 빈 화면이 열린다")
    void chatAlertCarriesRouterPath() {
        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificationSender).push(eq(12L), eq("chat"), payload.capture());

        /*
         * router/index.js 의 groupChallengeChat 이 /group-challenges/:id/chat 이다.
         * 예전 값 /challenge/group/... 은 어느 라우트에도 안 맞았는데, 이 자리를 any() 로만
         * 검증하고 있어서 아무도 몰랐다. 라우터는 매칭 실패를 예외로 알리지 않는다.
         */
        assertEquals("/group-challenges/" + GROUP_ID + "/chat", payload.getValue().get("deepLink"));
    }

    @Test
    @DisplayName("연타로 보내도 매 통 알림이 나간다 — 홈 배지·미리보기가 이 이벤트로만 갱신된다")
    void notifiesOnEveryMessage() {
        /*
         * 한때 같은 방·같은 사람에게 30초에 한 번만 보냈다(chat:notify-cd 쿨다운). 알림을 팝업으로만
         * 보던 시절의 도배 방지였는데, 이 이벤트가 그룹챌린지 홈의 데이터 채널이 되면서(#423)
         * **연타로 보낸 메시지가 홈에 안 뜨고 개수도 작게 남는** 원인이 됐다. 이슈 #423 에서 제거.
         */
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(SENDER_ID));

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");
        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕2");
        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕3");

        verify(notificationSender, times(3)).push(eq(12L), eq("chat"), any());
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

        service.postSystemMessage(GROUP_ID, trialSpec());

        InOrder order = inOrder(access, store);
        order.verify(access).memberIdsOf(GROUP_ID);
        order.verify(store).append(eq(GROUP_ID), eq(ChatMessageType.SYSTEM), any(), any(), anyString(),
                eq(ChatSystemType.TRIAL_OPENED), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("시스템 메시지도 접속 중인 사람의 배지·알림은 건너뛴다")
    void skipsUnreadAndNotificationForActiveUsersOnSystemMessage() {
        when(sessions.activeUserIds(GROUP_ID)).thenReturn(Set.of(9L));

        service.postSystemMessage(GROUP_ID, trialSpec());

        verify(store).increaseUnread(GROUP_ID, List.of(SENDER_ID, 12L));
        verify(events, never()).publishEvent(new NotificationRequestedEvent(
                9L, NotificationType.GROUP_TRIAL_OPENED, Map.of("content", "재판이 열렸어요"),
                "/group-challenges/7/trial"));
    }

    @Test
    @DisplayName("멤버가 없는 방에는 시스템 메시지를 저장하지 않는다")
    void skipsSystemMessageWhenNoMembers() {
        when(access.memberIdsOf(GROUP_ID)).thenReturn(Set.of());

        service.postSystemMessage(GROUP_ID, trialSpec());

        verify(store, never()).append(anyLong(), any(), any(), any(), anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("닉네임이 없어도 알림 루프가 끝까지 돈다")
    void continuesNotifyingWhenSenderNicknameIsNull() {
        when(store.append(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(ChatMessage.of(1L, ChatMessageType.TEXT, SENDER_ID, null,
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
        /* 쿨다운 조회를 실패 주입점으로 쓰다가 그것을 없애(#423) 실제 전송 자체가 터지는 쪽으로 옮겼다 */
        doThrow(new RuntimeException("전송 실패")).when(notificationSender)
                .push(eq(9L), anyString(), any());

        service.send(GROUP_ID, SENDER_ID, "절약왕", "안녕");

        verify(notificationSender).push(eq(12L), eq("chat"), any());
        verify(notificationSender).push(eq(15L), eq("chat"), any());
    }

    /** 시스템 메시지 스펙 — 값 자체는 이 테스트의 관심사가 아니라 한 곳에 모아 둔다 */
    private static ChatSystemMessageSpec trialSpec() {
        return new ChatSystemMessageSpec("재판이 열렸어요", ChatSystemType.TRIAL_OPENED,
                "/group-challenges/7/trial", "2026-재판-0001", NotificationType.GROUP_TRIAL_OPENED);
    }
}

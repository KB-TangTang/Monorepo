package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.service.NotificationSender;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 메시지 발송 파이프라인: 저장 → 브로드캐스트 → 안 읽은 수 → 알림.
 *
 * <p>알림이 두 갈래인 것이 이 클래스의 핵심이다(DECISIONS.md 2026-08-15).
 * <ul>
 *   <li>일반 채팅: 저장하지 않고 전달만 한다. 알림함에 남기지 않는다</li>
 *   <li>재판 시스템 메시지: NotificationRequestedEvent 로 저장까지 한다. 딥링크는 재판 상세다</li>
 * </ul>
 */
@Service
public class ChatMessageService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final ChatMessageStore store;
    private final ChatRoomAccessService access;
    private final ChatSessionRegistry sessions;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationSender notificationSender;
    private final ApplicationEventPublisher events;

    public ChatMessageService(ChatMessageStore store,
                              ChatRoomAccessService access,
                              ChatSessionRegistry sessions,
                              SimpMessagingTemplate messagingTemplate,
                              NotificationSender notificationSender,
                              ApplicationEventPublisher events) {
        this.store = store;
        this.access = access;
        this.sessions = sessions;
        this.messagingTemplate = messagingTemplate;
        this.notificationSender = notificationSender;
        this.events = events;
    }

    public ChatMessageDto send(long groupId, long senderId, String senderNickname, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("INVALID_REQUEST", "내용을 입력해 주세요.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("INVALID_REQUEST", "메시지가 너무 길어요.");
        }
        access.verifyCanEnter(groupId, senderId);

        ChatMessage saved = store.append(groupId, ChatMessageType.TEXT, senderId, senderNickname, content);
        ChatMessageDto dto = ChatMessageDto.from(saved);
        messagingTemplate.convertAndSend(destination(groupId), dto);

        List<Long> outsiders = outsiders(groupId, senderId);
        if (!outsiders.isEmpty()) {
            store.increaseUnread(groupId, outsiders);
            outsiders.forEach(userId -> pushChatAlert(groupId, userId, dto));
        }
        return dto;
    }

    /**
     * 재판 이벤트가 부르는 경로. 저장까지 하는 알림을 함께 낸다.
     *
     * <p>memberIdsOf 를 append 보다 먼저 호출한다. ChatMessageStore#append 는 그 방의 첫 메시지일 때
     * members 키(TTL 앵커)의 잔여 TTL 을 복제하는데, memberIdsOf 가 캐시를 데워 members 키를 만들어
     * 두지 않으면 이 방의 첫 메시지가 하필 시스템 메시지인 경우 TTL 없이 영구 보관된다.
     */
    public void postSystemMessage(long groupId, String content, String deepLink, NotificationType type) {
        Set<Long> members = access.memberIdsOf(groupId);

        ChatMessage saved = store.append(groupId, ChatMessageType.SYSTEM, null, null, content);
        messagingTemplate.convertAndSend(destination(groupId), ChatMessageDto.from(saved));

        store.increaseUnread(groupId, members);
        members.forEach(userId -> events.publishEvent(
                new NotificationRequestedEvent(userId, type, Map.of("content", content), deepLink)));
    }

    /** 발신자와 지금 방을 열어 둔 사람을 뺀 나머지 */
    private List<Long> outsiders(long groupId, long senderId) {
        Set<Long> active = sessions.activeUserIds(groupId);
        List<Long> result = new ArrayList<>();
        access.memberIdsOf(groupId).stream().sorted().forEach(userId -> {
            if (userId != senderId && !active.contains(userId)) {
                result.add(userId);
            }
        });
        return result;
    }

    /** 저장하지 않는다. 30초 쿨다운에 걸리면 배지만 올라가고 알림은 생략된다 */
    private void pushChatAlert(long groupId, long userId, ChatMessageDto message) {
        if (!store.tryAcquireNotifyCooldown(groupId, userId)) {
            return;
        }
        notificationSender.push(userId, "chat", Map.of(
                "groupId", groupId,
                "senderNickname", message.getSenderNickname(),
                "content", message.getContent(),
                "deepLink", "/challenge/group/" + groupId + "/chat"));
    }

    private String destination(long groupId) {
        return "/sub/chat/" + groupId;
    }
}

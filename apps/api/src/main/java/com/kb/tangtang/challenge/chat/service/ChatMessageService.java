package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.domain.ChatSystemMessageSpec;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 메시지 발송 파이프라인: 저장 → 브로드캐스트 → 안 읽은 수 → 알림.
 *
 * <p>알림이 두 갈래인 것이 이 클래스의 핵심이다(DECISIONS.md 2026-08-15).
 * <ul>
 *   <li>일반 채팅: 저장하지 않고 전달만 한다. 알림함에 남기지 않는다</li>
 *   <li>재판 시스템 메시지: NotificationRequestedEvent 로 저장까지 한다. 딥링크는 재판 상세다</li>
 * </ul>
 *
 * <p>⚠ <b>여기에 {@code SimpMessagingTemplate} 을 주입하지 마라.</b> 이 클래스는 루트 컨텍스트 빈이고
 * 그 템플릿은 서블릿(자식) 컨텍스트에만 있다 — 주입하면 루트 컨텍스트 refresh 가 실패해 war 가
 * 아예 뜨지 않는다. 전송은 {@link ChatBroadcaster} 를 통해서만 한다({@link ChatBroadcaster} 주석 참고).
 */
@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private static final int MAX_CONTENT_LENGTH = 500;

    /** 닉네임 온보딩을 아직 마치지 않은 발신자(nickname IS NULL)의 알림 문구에 쓰는 표시 문자열 */
    private static final String UNKNOWN_SENDER_NICKNAME = "이름을 정하지 않은 참여자";

    private final ChatMessageStore store;
    private final ChatRoomAccessService access;
    private final ChatSessionRegistry sessions;
    private final ChatBroadcaster broadcaster;
    private final NotificationSender notificationSender;
    private final ApplicationEventPublisher events;

    public ChatMessageService(ChatMessageStore store,
                              ChatRoomAccessService access,
                              ChatSessionRegistry sessions,
                              ChatBroadcaster broadcaster,
                              NotificationSender notificationSender,
                              ApplicationEventPublisher events) {
        this.store = store;
        this.access = access;
        this.sessions = sessions;
        this.broadcaster = broadcaster;
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
        broadcaster.broadcast(groupId, dto);

        List<Long> outsiders = outsiders(groupId, senderId);
        if (!outsiders.isEmpty()) {
            store.increaseUnread(groupId, outsiders);
            forEachSafely(outsiders, userId -> pushChatAlert(groupId, userId, dto), "채팅 알림 전송", groupId);
        }
        return dto;
    }

    /**
     * 재판 이벤트가 부르는 경로. 저장까지 하는 알림을 함께 낸다.
     *
     * <p>memberIdsOf 를 append 보다 먼저 호출한다. ChatMessageStore#append 는 그 방의 첫 메시지일 때
     * members 키(TTL 앵커)의 잔여 TTL 을 복제하는데, memberIdsOf 가 캐시를 데워 members 키를 만들어
     * 두지 않으면 이 방의 첫 메시지가 하필 시스템 메시지인 경우 TTL 없이 영구 보관된다.
     *
     * <p>멤버가 비어 있으면(존재하지 않거나 아직 아무도 캐시되지 않은 groupId) 그대로 반환한다 —
     * TTL 앵커(members 키) 없이 append 하면 messages·seq 키가 TTL 없이 영구 보관된다.
     *
     * <p>지금 방을 보고 있는 사람은 {@code send()} 와 같은 규칙으로 배지·알림 대상에서 뺀다.
     * 판결 메시지가 도착하는 순간 화면으로 이미 보고 있는데 배지가 올라가면 재입장 트리거가 없어
     * 영구히 남는다.
     */
    public void postSystemMessage(long groupId, ChatSystemMessageSpec spec) {
        Set<Long> members = access.memberIdsOf(groupId);
        if (members.isEmpty()) {
            return;
        }

        ChatMessage saved = store.append(groupId, ChatMessageType.SYSTEM, null, null,
                spec.content(), spec.systemType(), spec.deepLink(), spec.caseNo(), spec.verdict());
        broadcaster.broadcast(groupId, ChatMessageDto.from(saved));

        List<Long> outsiders = outsiders(members, groupId, null);
        if (!outsiders.isEmpty()) {
            store.increaseUnread(groupId, outsiders);
            forEachSafely(outsiders, userId -> events.publishEvent(
                    new NotificationRequestedEvent(userId, spec.notificationType(),
                            Map.of("content", spec.content()), spec.deepLink())),
                    "시스템 메시지 알림 발행", groupId);
        }
    }

    /** send() 가 쓰는 진입점. 매번 memberIdsOf 를 다시 조회한다(발신자 제외분) */
    private List<Long> outsiders(long groupId, long senderId) {
        return outsiders(access.memberIdsOf(groupId), groupId, senderId);
    }

    /**
     * members 중 발신자와 지금 방을 열어 둔 사람을 뺀 나머지.
     * senderId 가 null 이면(시스템 메시지) 발신자 제외 없이 접속자만 뺀다.
     */
    private List<Long> outsiders(Set<Long> members, long groupId, Long senderId) {
        Set<Long> active = sessions.activeUserIds(groupId);
        List<Long> result = new ArrayList<>();
        members.stream().sorted().forEach(userId -> {
            if (!Objects.equals(userId, senderId) && !active.contains(userId)) {
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
                "senderNickname", displayNickname(message.getSenderNickname()),
                "content", message.getContent(),
                // router/index.js 의 groupChallengeChat 경로와 글자 그대로 같아야 한다.
                // 예전 값 /challenge/group/... 은 어느 라우트에도 안 맞아 알림을 눌러도 빈 화면이었다.
                "deepLink", "/group-challenges/" + groupId + "/chat"));
    }

    /** 닉네임 온보딩 전(null)이어도 알림 문구가 깨지지 않도록 하는 표시 문자열 결정 지점 — 한 곳에만 둔다 */
    private String displayNickname(String senderNickname) {
        return senderNickname != null && !senderNickname.isBlank() ? senderNickname : UNKNOWN_SENDER_NICKNAME;
    }

    /**
     * 수신자별로 action 을 실행하되, 한 명의 실패(예: Redis 쿨다운 조회 예외)가 나머지 수신자를
     * 막지 않게 한다. 실패는 삼키지 않고 로그로 남기되 메시지 내용·토큰 등 개인정보는 찍지 않는다.
     */
    private void forEachSafely(Collection<Long> userIds, Consumer<Long> action, String actionName, long groupId) {
        userIds.forEach(userId -> {
            try {
                action.accept(userId);
            } catch (Exception e) {
                log.error("{} 실패 groupId={} userId={}", actionName, groupId, userId, e);
            }
        });
    }
}

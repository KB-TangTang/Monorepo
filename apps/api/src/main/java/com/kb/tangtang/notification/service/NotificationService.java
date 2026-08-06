package com.kb.tangtang.notification.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationDto;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 알림 저장·조회·읽음 처리. 트랜잭션 경계는 여기다.
 *
 * 커서는 id 를 쓴다. created_at 은 같은 초에 여러 건이 생기면 경계가 흔들린다.
 */
@Service
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final NotificationMapper mapper;

    public NotificationService(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public Notification create(long userId, NotificationType type, String content, String deepLinkUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.name())
                .title(type.getDefaultTitle())
                .content(content)
                .deepLinkUrl(deepLinkUrl)
                .build();
        mapper.insert(notification);
        return notification;
    }

    @Transactional(readOnly = true)
    public NotificationListDto list(long userId, Long cursor, Integer size) {
        int limit = clamp(size);
        List<Notification> rows = mapper.findPage(userId, cursor, limit);
        /* 페이지가 꽉 찼을 때만 다음이 있을 수 있다. 한 건 더 읽지 않는 대신 마지막 페이지에서 한 번 헛걸음한다 */
        String nextCursor = rows.size() == limit && !rows.isEmpty()
                ? String.valueOf(rows.get(rows.size() - 1).getId())
                : null;
        return NotificationListDto.builder()
                .items(rows.stream().map(this::toDto).toList())
                .nextCursor(nextCursor)
                .unreadCount(mapper.countUnread(userId))
                .build();
    }

    @Transactional
    public UnreadCountDto markRead(long userId, long id) {
        /* 없는 알림과 남의 알림을 구분해 알려주지 않는다 — 구분하면 id 를 훑어 존재를 알아낼 수 있다 */
        if (mapper.markRead(id, userId) == 0) {
            throw new BusinessException("NOT_FOUND", "알림을 찾을 수 없어요.");
        }
        return UnreadCountDto.builder().unreadCount(mapper.countUnread(userId)).build();
    }

    @Transactional
    public UnreadCountDto markAllRead(long userId) {
        mapper.markAllRead(userId);
        return UnreadCountDto.builder().unreadCount(mapper.countUnread(userId)).build();
    }

    @Transactional(readOnly = true)
    public UnreadCountDto unreadCount(long userId) {
        return UnreadCountDto.builder().unreadCount(mapper.countUnread(userId)).build();
    }

    private int clamp(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .deepLinkUrl(n.getDeepLinkUrl())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt() == null ? null
                        : n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}

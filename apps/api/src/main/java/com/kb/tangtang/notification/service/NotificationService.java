package com.kb.tangtang.notification.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationDto;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final Clock clock;

    /*
     * ⚠ 생성자가 둘이다. 어느 쪽을 쓸지 @Autowired 로 명시해 둔다 —
     *   후보가 둘이면 Spring 이 고르지 못해 컨텍스트 로딩이 실패한다.
     *   (NotificationDlqRetryScheduler·LinkProgressStore 와 같은 이유)
     */
    @Autowired
    public NotificationService(NotificationMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    public NotificationService(NotificationMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * 알림을 저장하고 **완성된** 객체를 돌려준다.
     *
     * ⚠ createdAt 을 여기서 채운다. DB 컬럼 기본값(CURRENT_TIMESTAMP)에 맡기면
     *   useGeneratedKeys 가 id 만 되받아 와 반환 객체의 createdAt 이 null 로 남는다.
     *   그대로 SSE 로 나가면 프론트가 1970-01-01 그룹에 얹는다.
     */
    @Transactional
    public Notification create(long userId, NotificationType type, String content, String deepLinkUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type.name())
                .title(type.getDefaultTitle())
                .content(content)
                .deepLinkUrl(deepLinkUrl)
                .isRead(false)
                .createdAt(LocalDateTime.now(clock).withNano(0))
                .build();
        mapper.insert(notification);
        return notification;
    }

    /**
     * 같은 알림이 이미 안 읽은 채로 있으면 만들지 않는다 (이슈 #70).
     *
     * 「즉시 조회」를 반복하면 같은 재연동 알림이 누를 때마다 쌓이던 문제를 막는다.
     * 판정 기준은 **종류 + 딥링크**다 — 계좌마다 알림이 따로 가야 하므로 종류만 보면 안 된다.
     * 이미 읽은 알림은 막지 않는다. 사용자가 확인했는데도 문제가 남아 있으면 다시 알릴 값어치가 있다.
     *
     * @return 만들었으면 그 알림, 중복이라 건너뛰었으면 비어 있음
     */
    @Transactional
    public Optional<Notification> createUnlessDuplicate(long userId, NotificationType type,
                                                        String content, String deepLinkUrl) {
        if (mapper.countUnreadSame(userId, type.name(), deepLinkUrl) > 0) {
            return Optional.empty();
        }
        return Optional.of(create(userId, type, content, deepLinkUrl));
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

    /** 변환은 NotificationDto.from 하나뿐이다 — SSE(NotificationSender)도 같은 것을 쓴다. */
    private NotificationDto toDto(Notification n) {
        return NotificationDto.from(n);
    }
}

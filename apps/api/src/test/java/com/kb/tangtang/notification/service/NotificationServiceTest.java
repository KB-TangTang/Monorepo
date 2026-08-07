package com.kb.tangtang.notification.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationServiceTest {

    /** 매퍼 페이크. 이 프로젝트는 mockito 의존성이 없어 익명 구현으로 대신한다 */
    private static class FakeMapper implements NotificationMapper {
        final List<Notification> saved = new ArrayList<>();
        List<Notification> page = List.of();
        int unread = 0;
        int markReadResult = 1;
        int markAllReadResult = 0;

        @Override public int insert(Notification n) { saved.add(n); return 1; }
        @Override public List<Notification> findPage(long userId, Long cursor, int size) { return page; }
        @Override public Notification findById(long id, long userId) { return null; }
        @Override public int countUnread(long userId) { return unread; }
        @Override public int markRead(long id, long userId) { return markReadResult; }
        @Override public int markAllRead(long userId) { return markAllReadResult; }
    }

    private Notification row(long id) {
        return Notification.builder()
                .id(id).userId(1L).type("ACCOUNT_RECONNECT")
                .title("계좌 재연동이 필요해요").content("국민은행 · 인증이 만료됐어요")
                .deepLinkUrl("/asset/accounts/9/reconnect")
                .isRead(false).createdAt(LocalDateTime.of(2026, 8, 6, 9, 0))
                .build();
    }

    @Test
    @DisplayName("종류의 기본 제목으로 저장한다")
    void createUsesDefaultTitle() {
        FakeMapper mapper = new FakeMapper();
        NotificationService service = new NotificationService(mapper);

        service.create(1L, NotificationType.ACCOUNT_RECONNECT, "국민은행 · 인증이 만료됐어요",
                "/asset/accounts/9/reconnect");

        assertEquals(1, mapper.saved.size());
        assertEquals("ACCOUNT_RECONNECT", mapper.saved.get(0).getType());
        assertEquals("계좌 재연동이 필요해요", mapper.saved.get(0).getTitle());
    }

    /**
     * ⚠ created_at 을 DB 컬럼 기본값에 맡기면 useGeneratedKeys 가 id 만 되받아 와
     *   반환 객체의 createdAt 이 null 로 남는다. 그 객체가 그대로 SSE 로 나가면
     *   프론트가 1970-01-01 그룹에 얹는다.
     */
    @Test
    @DisplayName("create 가 돌려주는 객체에는 createdAt 이 채워져 있다")
    void createFillsCreatedAt() {
        FakeMapper mapper = new FakeMapper();
        LocalDateTime fixed = LocalDateTime.of(2026, 8, 6, 9, 0);
        Clock clock = Clock.fixed(fixed.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        Notification created = new NotificationService(mapper, clock).create(
                1L, NotificationType.ACCOUNT_RECONNECT, "국민은행", "/asset/accounts/9/reconnect");

        assertEquals(fixed, created.getCreatedAt(), "반환 객체의 createdAt 이 비어 있다");
        assertEquals(fixed, mapper.saved.get(0).getCreatedAt(), "INSERT 에도 같은 값이 실려야 한다");
        assertFalse(created.isRead());
    }

    @Test
    @DisplayName("size 는 1~50 으로 자른다 — 초과 요청은 에러가 아니라 50 이다")
    void clampsSize() {
        FakeMapper mapper = new FakeMapper() {
            @Override public List<Notification> findPage(long userId, Long cursor, int size) {
                assertEquals(50, size);
                return List.of();
            }
        };
        new NotificationService(mapper).list(1L, null, 999);
    }

    @Test
    @DisplayName("더 가져올 게 없으면 nextCursor 는 null 이다")
    void nextCursorNullWhenPageNotFull() {
        FakeMapper mapper = new FakeMapper();
        mapper.page = List.of(row(10L));
        mapper.unread = 1;

        NotificationListDto result = new NotificationService(mapper).list(1L, null, 20);

        assertEquals(1, result.getItems().size());
        assertNull(result.getNextCursor());
        assertEquals(1, result.getUnreadCount());
    }

    @Test
    @DisplayName("페이지가 꽉 차면 nextCursor 는 마지막 id 다")
    void nextCursorIsLastId() {
        FakeMapper mapper = new FakeMapper();
        mapper.page = List.of(row(10L), row(9L));

        NotificationListDto result = new NotificationService(mapper).list(1L, null, 2);

        assertEquals("9", result.getNextCursor());
    }

    @Test
    @DisplayName("없거나 남의 알림을 읽으려 하면 NOT_FOUND — 존재 여부를 구분해 알려주지 않는다")
    void markReadOnOthersNotification() {
        FakeMapper mapper = new FakeMapper();
        mapper.markReadResult = 0;

        BusinessException e = assertThrows(BusinessException.class,
                () -> new NotificationService(mapper).markRead(1L, 999L));
        assertEquals("NOT_FOUND", e.getCode());
    }
}

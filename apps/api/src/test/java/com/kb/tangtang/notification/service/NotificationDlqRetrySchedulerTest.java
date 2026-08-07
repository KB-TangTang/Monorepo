package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDlqRetrySchedulerTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 6, 9, 0);

    private static class FakeDlqMapper implements NotificationDlqMapper {
        List<NotificationDlqRow> rows = new ArrayList<>();
        final List<Long> increased = new ArrayList<>();
        final List<Long> deleted = new ArrayList<>();
        @Override public int insert(String a, String b, String c) { return 1; }
        @Override public List<NotificationDlqRow> findRetryable(int maxRetry) {
            return rows.stream().filter(r -> r.getRetryCount() < maxRetry).toList();
        }
        @Override public int increaseRetry(long dlqId) { increased.add(dlqId); return 1; }
        @Override public int delete(long dlqId) { deleted.add(dlqId); return 1; }
    }

    private static class FakeNotificationMapper implements NotificationMapper {
        Notification stored;
        @Override public int insert(Notification n) { return 1; }
        @Override public List<Notification> findPage(long u, Long c, int s) { return List.of(); }
        @Override public int countUnread(long u) { return 0; }
        @Override public int markRead(long i, long u) { return 1; }
        @Override public int markAllRead(long u) { return 1; }
        @Override public Notification findById(long id, long userId) {
            return stored != null && stored.getId() == id ? stored : null;
        }
    }

    private static class RecordingService extends NotificationService {
        final List<String> created = new ArrayList<>();
        boolean explode = false;
        RecordingService() { super((NotificationMapper) null); }
        @Override
        public Notification create(long userId, NotificationType type, String content, String deepLink) {
            if (explode) {
                throw new IllegalStateException("DB down");
            }
            created.add(userId + "|" + type + "|" + content + "|" + deepLink);
            return Notification.builder().id(99L).userId(userId).type(type.name())
                    .content(content).deepLinkUrl(deepLink).createdAt(BASE).build();
        }
        @Override public NotificationListDto list(long u, Long c, Integer s) { return null; }
        @Override public UnreadCountDto markRead(long u, long i) { return null; }
        @Override public UnreadCountDto markAllRead(long u) { return null; }
    }

    /** 실제 전송 대신 성공/실패만 흉내 낸다. DLQ 를 다시 쌓지 않는지도 여기서 본다. */
    private static class FakeSender extends NotificationSender {
        final List<Long> sent = new ArrayList<>();
        final List<String> extraDlq = new ArrayList<>();
        boolean deliver = true;
        FakeSender(NotificationDlqMapper dlq) { super(new SseEmitterRegistry(), dlq); }
        @Override public boolean trySend(Notification n) { sent.add(n.getId()); return deliver; }
        @Override public void sendFailure(String t, String p, String e) { extraDlq.add(t); }
    }

    private Clock clockAt(LocalDateTime at) {
        return Clock.fixed(at.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    private NotificationDlqRow row(long id, int retryCount) {
        return row(id, retryCount, "{}");
    }

    private NotificationDlqRow row(long id, int retryCount, String payloadJson) {
        return NotificationDlqRow.builder()
                .dlqId(id).originalEventType("ACCOUNT_RECONNECT").payloadJson(payloadJson)
                .retryCount(retryCount).regDate(BASE).build();
    }

    private NotificationDlqRetryScheduler scheduler(FakeDlqMapper dlq, FakeNotificationMapper mapper,
                                                    RecordingService service, FakeSender sender,
                                                    LocalDateTime at) {
        return new NotificationDlqRetryScheduler(dlq, mapper, service, sender, clockAt(at));
    }

    @Test
    @DisplayName("백오프는 1분 → 5분 → 15분 → 1시간 → 6시간 순으로 늘어난다")
    void backoffGrows() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        NotificationDlqRetryScheduler scheduler = scheduler(dlq, new FakeNotificationMapper(),
                new RecordingService(), new FakeSender(dlq), BASE);

        assertEquals(BASE.plusMinutes(1), scheduler.nextAttemptAt(row(1L, 0)));
        assertEquals(BASE.plusMinutes(6), scheduler.nextAttemptAt(row(1L, 1)));
        assertEquals(BASE.plusMinutes(21), scheduler.nextAttemptAt(row(1L, 2)));
        assertEquals(BASE.plusMinutes(81), scheduler.nextAttemptAt(row(1L, 3)));
        assertEquals(BASE.plusMinutes(441), scheduler.nextAttemptAt(row(1L, 4)));
    }

    @Test
    @DisplayName("다음 시각이 아직 안 됐으면 건드리지 않는다")
    void skipsRowsNotDueYet() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0));
        RecordingService service = new RecordingService();

        scheduler(dlq, new FakeNotificationMapper(), service, new FakeSender(dlq),
                BASE.plusSeconds(30)).retryDue();

        assertEquals(List.of(), dlq.increased);
        assertEquals(List.of(), dlq.deleted);
        assertTrue(service.created.isEmpty());
    }

    @Test
    @DisplayName("알림을 만들기 전에 실패한 행은 알림을 다시 만들고 성공하면 DLQ 행을 지운다")
    void recreatesNotificationAndDeletesRow() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0, """
                {"userId":7,"type":"ACCOUNT_RECONNECT","content":"국민은행 · 인증이 만료됐어요",\
                "deepLinkUrl":"/asset/accounts/9/reconnect"}"""));
        RecordingService service = new RecordingService();
        FakeSender sender = new FakeSender(dlq);

        scheduler(dlq, new FakeNotificationMapper(), service, sender, BASE.plusMinutes(2)).retryDue();

        assertEquals(List.of("7|ACCOUNT_RECONNECT|국민은행 · 인증이 만료됐어요|/asset/accounts/9/reconnect"),
                service.created);
        assertEquals(List.of(99L), sender.sent);
        assertEquals(List.of(1L), dlq.deleted, "성공했으면 DLQ 행이 남으면 안 된다");
        assertEquals(List.of(), dlq.increased);
    }

    @Test
    @DisplayName("이미 저장된 알림의 푸시 실패는 알림을 새로 만들지 않고 다시 보낸다")
    void resendsAlreadySavedNotification() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0, """
                {"notificationId":31,"userId":7}"""));
        FakeNotificationMapper mapper = new FakeNotificationMapper();
        mapper.stored = Notification.builder().id(31L).userId(7L)
                .type("ACCOUNT_RECONNECT").content("국민은행 · 인증이 만료됐어요").createdAt(BASE).build();
        RecordingService service = new RecordingService();
        FakeSender sender = new FakeSender(dlq);

        scheduler(dlq, mapper, service, sender, BASE.plusMinutes(2)).retryDue();

        assertTrue(service.created.isEmpty(), "이미 저장된 알림을 또 만들면 중복이 된다");
        assertEquals(List.of(31L), sender.sent);
        assertEquals(List.of(1L), dlq.deleted);
    }

    @Test
    @DisplayName("재시도가 또 실패하면 행을 지우지 않고 재시도 횟수만 올린다")
    void keepsRowWhenRetryFailsAgain() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0, """
                {"userId":7,"type":"ACCOUNT_RECONNECT","content":"국민은행 · 인증이 만료됐어요",\
                "deepLinkUrl":"/asset/accounts/9/reconnect"}"""));
        RecordingService service = new RecordingService();
        service.explode = true;
        FakeSender sender = new FakeSender(dlq);

        scheduler(dlq, new FakeNotificationMapper(), service, sender, BASE.plusMinutes(2)).retryDue();

        assertEquals(List.of(1L), dlq.increased);
        assertEquals(List.of(), dlq.deleted);
        assertEquals(List.of(), sender.extraDlq, "재시도 실패로 DLQ 행을 새로 만들면 안 된다");
    }

    @Test
    @DisplayName("사라진 알림을 가리키는 행은 계속 재시도하지 않고 지운다")
    void dropsRowWhenNotificationGone() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0, """
                {"notificationId":31,"userId":7}"""));

        scheduler(dlq, new FakeNotificationMapper(), new RecordingService(),
                new FakeSender(dlq), BASE.plusMinutes(2)).retryDue();

        assertEquals(List.of(1L), dlq.deleted);
        assertEquals(List.of(), dlq.increased);
    }

    @Test
    @DisplayName("payload 가 깨진 행이 있어도 배치가 멈추지 않고 나머지를 처리한다")
    void brokenPayloadDoesNotStopTheBatch() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(
                row(1L, 0, "이건 JSON 이 아니다"),
                row(2L, 0, """
                        {"userId":7,"type":"ACCOUNT_RECONNECT","content":"국민은행 · 인증이 만료됐어요",\
                        "deepLinkUrl":"/asset/accounts/9/reconnect"}"""));
        RecordingService service = new RecordingService();

        scheduler(dlq, new FakeNotificationMapper(), service, new FakeSender(dlq),
                BASE.plusMinutes(2)).retryDue();

        assertEquals(List.of(1L), dlq.increased, "깨진 행은 재시도 횟수만 올려 결국 대상에서 빠지게 한다");
        assertEquals(1, service.created.size(), "뒤 행은 정상 처리돼야 한다");
        assertEquals(List.of(2L), dlq.deleted);
    }

    @Test
    @DisplayName("다시 만들 정보가 없는 행은 재시도 횟수만 올린다")
    void unrebuildableRowJustAgesOut() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 0, "{}"));

        scheduler(dlq, new FakeNotificationMapper(), new RecordingService(),
                new FakeSender(dlq), BASE.plusMinutes(2)).retryDue();

        assertEquals(List.of(1L), dlq.increased);
        assertEquals(List.of(), dlq.deleted);
    }

    @Test
    @DisplayName("5회를 채운 행은 조회 대상에서 빠진다")
    void stopsAfterMaxRetry() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        dlq.rows = List.of(row(1L, 5));

        scheduler(dlq, new FakeNotificationMapper(), new RecordingService(),
                new FakeSender(dlq), BASE.plusDays(1)).retryDue();

        assertEquals(List.of(), dlq.increased);
        assertEquals(List.of(), dlq.deleted);
    }
}

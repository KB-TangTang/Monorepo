package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationDlqRetrySchedulerTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 6, 9, 0);

    private static class FakeDlqMapper implements NotificationDlqMapper {
        List<NotificationDlqRow> rows = new ArrayList<>();
        final List<Long> increased = new ArrayList<>();
        @Override public int insert(String a, String b, String c) { return 1; }
        @Override public List<NotificationDlqRow> findRetryable(int maxRetry) {
            return rows.stream().filter(r -> r.getRetryCount() < maxRetry).toList();
        }
        @Override public int increaseRetry(long dlqId) { increased.add(dlqId); return 1; }
        @Override public int delete(long dlqId) { return 1; }
    }

    private Clock clockAt(LocalDateTime at) {
        return Clock.fixed(at.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    private NotificationDlqRow row(long id, int retryCount) {
        return NotificationDlqRow.builder()
                .dlqId(id).originalEventType("NOTIFICATION_CREATE").payloadJson("{}")
                .retryCount(retryCount).regDate(BASE).build();
    }

    @Test
    @DisplayName("백오프는 1분 → 5분 → 15분 → 1시간 → 6시간 순으로 늘어난다")
    void backoffGrows() {
        NotificationDlqRetryScheduler scheduler =
                new NotificationDlqRetryScheduler(new FakeDlqMapper(), clockAt(BASE));

        assertEquals(BASE.plusMinutes(1), scheduler.nextAttemptAt(row(1L, 0)));
        assertEquals(BASE.plusMinutes(6), scheduler.nextAttemptAt(row(1L, 1)));
        assertEquals(BASE.plusMinutes(21), scheduler.nextAttemptAt(row(1L, 2)));
        assertEquals(BASE.plusMinutes(81), scheduler.nextAttemptAt(row(1L, 3)));
        assertEquals(BASE.plusMinutes(441), scheduler.nextAttemptAt(row(1L, 4)));
    }

    @Test
    @DisplayName("다음 시각이 아직 안 됐으면 건드리지 않는다")
    void skipsRowsNotDueYet() {
        FakeDlqMapper mapper = new FakeDlqMapper();
        mapper.rows = List.of(row(1L, 0));

        new NotificationDlqRetryScheduler(mapper, clockAt(BASE.plusSeconds(30))).retryDue();

        assertEquals(List.of(), mapper.increased);
    }

    @Test
    @DisplayName("시각이 도래하면 재시도 횟수를 올린다")
    void retriesDueRows() {
        FakeDlqMapper mapper = new FakeDlqMapper();
        mapper.rows = List.of(row(1L, 0));

        new NotificationDlqRetryScheduler(mapper, clockAt(BASE.plusMinutes(2))).retryDue();

        assertEquals(List.of(1L), mapper.increased);
    }

    @Test
    @DisplayName("5회에 도달한 행은 조회 대상에서 빠진다")
    void stopsAfterMaxRetry() {
        FakeDlqMapper mapper = new FakeDlqMapper();
        mapper.rows = List.of(row(1L, 5));

        new NotificationDlqRetryScheduler(mapper, clockAt(BASE.plusDays(1))).retryDue();

        assertEquals(List.of(), mapper.increased);
    }
}

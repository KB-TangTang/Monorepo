package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.mapper.FixedExpensePaymentReminderMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedExpensePaymentReminderBatchServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("오늘과 정확히 7일 뒤 결제만 알리고 지난·8일 뒤·비대상 항목은 제외한다")
    void sendsOnlyEligibleCandidatesWithinInclusiveSevenDayWindow() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.candidates = List.of(
                candidate(101L, "오늘결제", "ACTIVE", false, "2026-08-14", confirmedAt()),
                candidate(102L, "일주일결제", "ACTIVE", false, "2026-08-21", confirmedAt()),
                candidate(103L, "지난결제", "ACTIVE", false, "2026-08-13", confirmedAt()),
                candidate(104L, "8일뒤결제", "ACTIVE", false, "2026-08-22", confirmedAt()),
                candidate(105L, "미확정", "ACTIVE", false, "2026-08-20", null),
                candidate(106L, "제외", "ACTIVE", true, "2026-08-20", confirmedAt()),
                candidate(107L, "버퍼", "BUFFER", false, "2026-08-20", confirmedAt()),
                candidate(108L, "해지검증", "VERIFIED_CANCELLED", false, "2026-08-20", confirmedAt()));
        RecordingEvents events = new RecordingEvents();

        int published = service(mapper, events, "2026-08-13T15:00:00Z").sendDuePaymentReminders();

        assertEquals(2, published);
        assertEquals(LocalDate.of(2026, 8, 14), mapper.startDate);
        assertEquals(LocalDate.of(2026, 8, 21), mapper.endDate);
        assertEquals(2, events.requests.size());

        NotificationRequestedEvent today = events.requests.get(0);
        assertEquals("PAYMENT_DUE", today.type().name());
        assertEquals("오늘결제 · 오늘 결제 예정", today.params().get("content"));
        assertEquals("/asset/fixed-expenses/101?source=api", today.deepLinkUrl());

        NotificationRequestedEvent sevenDays = events.requests.get(1);
        assertEquals("PAYMENT_DUE", sevenDays.type().name());
        assertEquals("일주일결제 · 7일 후 결제 예정", sevenDays.params().get("content"));
        assertEquals("/asset/fixed-expenses/102?source=api", sevenDays.deepLinkUrl());
    }

    @Test
    @DisplayName("같은 예정일로 배치를 다시 돌려도 한 번만 발행하고 새 결제 주기에는 다시 발행한다")
    void registersOncePerCandidateAndExpectedPaymentDate() {
        RecordingMapper mapper = new RecordingMapper();
        RecordingEvents events = new RecordingEvents();
        mapper.candidates = List.of(candidate(101L, "구독서비스", "ACTIVE", false,
                "2026-08-21", confirmedAt()));

        FixedExpensePaymentReminderBatchService august = service(mapper, events, "2026-08-13T15:00:00Z");
        assertEquals(1, august.sendDuePaymentReminders());
        assertEquals(0, august.sendDuePaymentReminders());

        mapper.candidates = List.of(candidate(101L, "구독서비스", "ACTIVE", false,
                "2026-09-21", confirmedAt()));
        assertEquals(1, service(mapper, events, "2026-09-13T15:00:00Z").sendDuePaymentReminders());

        assertEquals(2, events.requests.size());
        assertEquals(Set.of("101|2026-08-21|PAYMENT_DUE", "101|2026-09-21|PAYMENT_DUE"),
                mapper.registeredKeys);
    }

    private FixedExpensePaymentReminderBatchService service(RecordingMapper mapper,
                                                            RecordingEvents events,
                                                            String instant) {
        return new FixedExpensePaymentReminderBatchService(
                mapper, events, Clock.fixed(Instant.parse(instant), SEOUL));
    }

    private FixedExpenseCandidate candidate(long id, String merchant, String status, boolean excluded,
                                            String expectedDate, LocalDateTime confirmedAt) {
        return FixedExpenseCandidate.builder()
                .id(id)
                .userId(7L)
                .merchantNameNormalized(merchant)
                .isExcluded(excluded)
                .status(status)
                .nextExpectedDate(LocalDate.parse(expectedDate))
                .confirmedAt(confirmedAt)
                .build();
    }

    private LocalDateTime confirmedAt() {
        return LocalDateTime.of(2026, 7, 1, 9, 0);
    }

    private static class RecordingMapper implements FixedExpensePaymentReminderMapper {
        private List<FixedExpenseCandidate> candidates = List.of();
        private final Set<String> registeredKeys = new HashSet<>();
        private LocalDate startDate;
        private LocalDate endDate;

        @Override
        public List<FixedExpenseCandidate> findPaymentReminderCandidates(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return candidates;
        }

        @Override
        public int registerPaymentReminder(long candidateId, LocalDate expectedPaymentDate,
                                           String notificationType) {
            return registeredKeys.add(candidateId + "|" + expectedPaymentDate + "|" + notificationType) ? 1 : 0;
        }

        @Override
        public int deletePaymentRemindersByUser(long userId) {
            return 0;
        }
    }

    private static class RecordingEvents implements ApplicationEventPublisher {
        private final List<NotificationRequestedEvent> requests = new ArrayList<>();

        @Override
        public void publishEvent(ApplicationEvent event) {
            throw new UnsupportedOperationException("NotificationRequestedEvent is published through Object overload");
        }

        @Override
        public void publishEvent(Object event) {
            requests.add((NotificationRequestedEvent) event);
        }
    }
}

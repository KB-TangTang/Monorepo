package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.mapper.FixedExpensePaymentReminderMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** 확정 고정지출의 결제 예정일 7일 전부터 결제 주기별로 한 번만 알린다. */
@Service
@Log4j2
public class FixedExpensePaymentReminderBatchService {

    private static final String ACTIVE = "ACTIVE";
    private static final int REMINDER_WINDOW_DAYS = 7;

    private final FixedExpensePaymentReminderMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public FixedExpensePaymentReminderBatchService(
            FixedExpensePaymentReminderMapper mapper,
            ApplicationEventPublisher events,
            @Value("${fixed.expense.detection.zone:Asia/Seoul}") String zoneId) {
        this(mapper, events, Clock.system(ZoneId.of(zoneId)));
    }

    FixedExpensePaymentReminderBatchService(FixedExpensePaymentReminderMapper mapper,
                                            ApplicationEventPublisher events,
                                            Clock clock) {
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    /** 대상 조회·발송 이력 등록·이벤트 발행을 하나의 트랜잭션으로 처리한다. */
    @Transactional
    public int sendDuePaymentReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusDays(REMINDER_WINDOW_DAYS);
        List<FixedExpenseCandidate> candidates = mapper.findPaymentReminderCandidates(today, endDate);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (FixedExpenseCandidate candidate : candidates) {
            if (!isReminderTarget(candidate, today, endDate)) {
                continue;
            }

            LocalDate expectedPaymentDate = candidate.getNextExpectedDate();
            int registered = mapper.registerPaymentReminder(
                    candidate.getId(), expectedPaymentDate, NotificationType.PAYMENT_DUE.name());
            if (registered != 1) {
                continue;
            }

            events.publishEvent(new NotificationRequestedEvent(
                    candidate.getUserId(),
                    NotificationType.PAYMENT_DUE,
                    Map.of("content", reminderContent(candidate, today)),
                    deepLink(candidate.getId())));
            publishedCount++;
        }

        log.info("고정지출 결제 예정 알림 배치 완료. published={}", publishedCount);
        return publishedCount;
    }

    private boolean isReminderTarget(FixedExpenseCandidate candidate,
                                     LocalDate today,
                                     LocalDate endDate) {
        if (candidate == null
                || candidate.getId() == null
                || candidate.getUserId() == null
                || candidate.getMerchantNameNormalized() == null
                || candidate.getMerchantNameNormalized().isBlank()
                || candidate.isExcluded()
                || !ACTIVE.equals(candidate.getStatus())
                || candidate.getConfirmedAt() == null
                || candidate.getNextExpectedDate() == null) {
            return false;
        }
        LocalDate expectedPaymentDate = candidate.getNextExpectedDate();
        return !expectedPaymentDate.isBefore(today) && !expectedPaymentDate.isAfter(endDate);
    }

    private String reminderContent(FixedExpenseCandidate candidate, LocalDate today) {
        long daysUntilPayment = ChronoUnit.DAYS.between(today, candidate.getNextExpectedDate());
        String dueText = daysUntilPayment == 0
                ? "오늘 결제 예정"
                : daysUntilPayment + "일 후 결제 예정";
        return candidate.getMerchantNameNormalized() + " · " + dueText;
    }

    private String deepLink(long candidateId) {
        return "/asset/fixed-expenses/" + candidateId;
    }
}

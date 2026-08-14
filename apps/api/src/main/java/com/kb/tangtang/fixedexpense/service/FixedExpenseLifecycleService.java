package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionRule;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseLifecycleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** 확정 고정지출의 BUFFER·해지 검증과 BUFFER 재결제 복귀를 담당한다. */
@Service
public class FixedExpenseLifecycleService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_AMOUNT_TOLERANCE_PCT = BigDecimal.TEN;
    private static final String ACTIVE = "ACTIVE";
    private static final String BUFFER = "BUFFER";

    private final FixedExpenseLifecycleMapper lifecycleMapper;
    private final FixedExpenseDetectionMapper detectionMapper;
    private final Clock clock;

    @Autowired
    public FixedExpenseLifecycleService(
            FixedExpenseLifecycleMapper lifecycleMapper,
            FixedExpenseDetectionMapper detectionMapper,
            @Value("${fixed.expense.detection.zone:Asia/Seoul}") String zoneId) {
        this(lifecycleMapper, detectionMapper, Clock.system(ZoneId.of(zoneId)));
    }

    FixedExpenseLifecycleService(FixedExpenseLifecycleMapper lifecycleMapper,
                                 FixedExpenseDetectionMapper detectionMapper,
                                 Clock clock) {
        this.lifecycleMapper = lifecycleMapper;
        this.detectionMapper = detectionMapper;
        this.clock = clock;
    }

    /** 한 사용자의 확정 항목을 검증한다. 모든 실제 상태 변경은 조건부 UPDATE로 보호한다. */
    @Transactional
    public int verifyForUser(long userId) {
        FixedExpenseDetectionRule rule = detectionMapper.findActiveRule();
        if (rule == null) {
            return 0;
        }

        List<FixedExpenseCandidate> candidates = lifecycleMapper.findLifecycleCandidates(userId);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now(clock);
        int changedCount = 0;

        // 이번 실행에서 새 BUFFER가 된 항목은 다음 검증 기간까지 취소 검증하지 않는다.
        for (FixedExpenseCandidate candidate : candidates) {
            if (BUFFER.equals(candidate.getStatus())) {
                changedCount += verifyBufferedCandidate(userId, candidate, rule, today);
            }
        }
        for (FixedExpenseCandidate candidate : candidates) {
            if (ACTIVE.equals(candidate.getStatus()) && candidate.getConfirmedAt() != null) {
                changedCount += verifyConfirmedCandidate(userId, candidate, rule, today);
            }
        }
        return changedCount;
    }

    private int verifyConfirmedCandidate(long userId,
                                         FixedExpenseCandidate candidate,
                                         FixedExpenseDetectionRule rule,
                                         LocalDate today) {
        LocalDate expectedDate = candidate.getNextExpectedDate();
        if (expectedDate == null) {
            return 0;
        }

        PaymentWindow window = paymentWindow(expectedDate, rule);
        FixedExpenseDetectionTransaction payment = findMatchingPayment(userId, candidate, rule, window, today);
        if (payment != null) {
            LocalDate nextExpectedDate = payment.getTrDate().plusMonths(1);
            int updated = lifecycleMapper.advanceConfirmedCandidate(
                    candidate.getId(), userId, expectedDate, nextExpectedDate);
            linkPaymentWhenUpdated(updated, payment, userId, candidate.getId());
            return updated;
        }
        if (!today.isAfter(window.endDate())) {
            return 0;
        }

        return lifecycleMapper.moveConfirmedCandidateToBuffer(
                candidate.getId(), userId, expectedDate, expectedDate.plusMonths(1));
    }

    private int verifyBufferedCandidate(long userId,
                                        FixedExpenseCandidate candidate,
                                        FixedExpenseDetectionRule rule,
                                        LocalDate today) {
        LocalDate expectedDate = candidate.getNextExpectedDate();
        if (expectedDate == null) {
            return 0;
        }

        PaymentWindow window = paymentWindow(expectedDate, rule);
        FixedExpenseDetectionTransaction payment = findMatchingPayment(userId, candidate, rule, window, today);
        if (payment != null) {
            LocalDateTime detectedAt = LocalDateTime.now(clock).withNano(0);
            int updated = lifecycleMapper.reactivateBufferedCandidate(
                    candidate.getId(), userId, expectedDate, payment.getTrDate().plusMonths(1), detectedAt);
            linkPaymentWhenUpdated(updated, payment, userId, candidate.getId());
            return updated;
        }
        if (!today.isAfter(window.endDate())) {
            return 0;
        }

        return lifecycleMapper.verifyBufferedCandidate(
                candidate.getId(), userId, expectedDate, LocalDateTime.now(clock).withNano(0));
    }

    private FixedExpenseDetectionTransaction findMatchingPayment(long userId,
                                                                  FixedExpenseCandidate candidate,
                                                                  FixedExpenseDetectionRule rule,
                                                                  PaymentWindow window,
                                                                  LocalDate today) {
        if (today.isBefore(window.startDate())) {
            return null;
        }
        LocalDate endDate = today.isBefore(window.endDate()) ? today : window.endDate();
        List<FixedExpenseDetectionTransaction> payments = lifecycleMapper.findEligiblePayments(
                userId, candidate.getMerchantNameNormalized(), window.startDate(), endDate);
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.stream()
                .filter(payment -> isAmountWithinTolerance(payment, candidate, rule))
                .max(Comparator.comparing(FixedExpenseDetectionTransaction::getTrDate)
                        .thenComparing(FixedExpenseDetectionTransaction::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean isAmountWithinTolerance(FixedExpenseDetectionTransaction payment,
                                            FixedExpenseCandidate candidate,
                                            FixedExpenseDetectionRule rule) {
        if (payment == null || payment.getAmount() == null || payment.getAmount().signum() <= 0
                || candidate.getAvgAmount() == null || candidate.getAvgAmount().signum() <= 0) {
            return false;
        }
        BigDecimal tolerancePct = rule.getAmountTolerancePct() == null
                ? DEFAULT_AMOUNT_TOLERANCE_PCT
                : rule.getAmountTolerancePct();
        BigDecimal allowedDifference = candidate.getAvgAmount()
                .multiply(tolerancePct)
                .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
        return payment.getAmount().subtract(candidate.getAvgAmount()).abs()
                .compareTo(allowedDifference) <= 0;
    }

    private PaymentWindow paymentWindow(LocalDate expectedDate, FixedExpenseDetectionRule rule) {
        int toleranceDays = Math.max(0, rule.getCycleToleranceDays());
        return new PaymentWindow(expectedDate.minusDays(toleranceDays), expectedDate.plusDays(toleranceDays));
    }

    private void linkPaymentWhenUpdated(int updated,
                                        FixedExpenseDetectionTransaction payment,
                                        long userId,
                                        long candidateId) {
        if (updated == 1 && payment.getId() != null) {
            lifecycleMapper.linkPaymentToCandidate(payment.getId(), userId, candidateId);
        }
    }

    private record PaymentWindow(LocalDate startDate, LocalDate endDate) {
    }
}

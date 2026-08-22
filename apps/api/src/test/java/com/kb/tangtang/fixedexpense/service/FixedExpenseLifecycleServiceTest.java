package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionRule;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseLifecycleMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FixedExpenseLifecycleServiceTest {

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void reexposesJanuaryToMaySubscriptionAsCandidateAfterJuneMissAndOneJulyPayment() {
        RecordingLifecycleMapper mapper = new RecordingLifecycleMapper();
        mapper.put(candidate(100L, USER_ID, "넷플릭스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));

        assertEquals(1, service(mapper, clock("2026-06-23T00:00:00Z")).verifyForUser(USER_ID));

        FixedExpenseCandidate buffered = mapper.candidate(100L);
        assertEquals("BUFFER", buffered.getStatus());
        assertEquals(LocalDate.of(2026, 7, 15), buffered.getNextExpectedDate());
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 0), buffered.getConfirmedAt());

        mapper.payments.add(payment(501L, USER_ID, "넷플릭스", "2026-07-15", "10000"));

        assertEquals(1, service(mapper, clock("2026-07-15T00:00:00Z")).verifyForUser(USER_ID));

        FixedExpenseCandidate reactivated = mapper.candidate(100L);
        assertEquals("ACTIVE", reactivated.getStatus());
        assertNull(reactivated.getConfirmedAt());
        assertEquals(LocalDateTime.of(2026, 7, 15, 9, 0), reactivated.getRelapseDetectedAt());
        assertEquals(LocalDate.of(2026, 8, 15), reactivated.getNextExpectedDate());
        assertEquals(List.of(501L), mapper.linkedTransactionIds);
    }

    @Test
    void verifiesCancellationOnlyAfterTheFollowingBufferWindowExpires() {
        RecordingLifecycleMapper mapper = new RecordingLifecycleMapper();
        mapper.put(candidate(100L, USER_ID, "음악서비스", "BUFFER", "2026-07-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));

        assertEquals(0, service(mapper, clock("2026-07-22T00:00:00Z")).verifyForUser(USER_ID));
        assertEquals("BUFFER", mapper.candidate(100L).getStatus());

        assertEquals(1, service(mapper, clock("2026-07-23T00:00:00Z")).verifyForUser(USER_ID));
        assertEquals("VERIFIED_CANCELLED", mapper.candidate(100L).getStatus());
        assertEquals(LocalDateTime.of(2026, 7, 23, 9, 0), mapper.verifiedAtByCandidateId.get(100L));
    }

    @Test
    void ignoresUnconfirmedExcludedAndOtherUserCandidatesAndPayments() {
        RecordingLifecycleMapper mapper = new RecordingLifecycleMapper();
        mapper.put(candidate(100L, USER_ID, "확정서비스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));
        mapper.put(candidate(101L, USER_ID, "후보서비스", "ACTIVE", "2026-06-15", null, false));
        mapper.put(candidate(102L, USER_ID, "제외서비스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), true));
        mapper.put(candidate(103L, OTHER_USER_ID, "다른사용자서비스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));
        mapper.payments.add(payment(501L, OTHER_USER_ID, "확정서비스", "2026-06-15", "10000"));

        assertEquals(1, service(mapper, clock("2026-06-23T00:00:00Z")).verifyForUser(USER_ID));

        assertEquals("BUFFER", mapper.candidate(100L).getStatus());
        assertEquals("ACTIVE", mapper.candidate(101L).getStatus());
        assertEquals("ACTIVE", mapper.candidate(102L).getStatus());
        assertEquals("ACTIVE", mapper.candidate(103L).getStatus());
        assertTrue(mapper.linkedTransactionIds.isEmpty());
    }

    @Test
    void rejectsOutOfTolerancePaymentAndHandlesLeapDayAllowanceBoundary() {
        RecordingLifecycleMapper mapper = new RecordingLifecycleMapper();
        mapper.put(candidate(100L, USER_ID, "윤년서비스", "ACTIVE", "2028-02-29",
                LocalDateTime.of(2028, 1, 31, 9, 0), false));
        mapper.payments.add(payment(501L, USER_ID, "윤년서비스", "2028-02-29", "12000"));

        assertEquals(0, service(mapper, clock("2028-03-07T00:00:00Z")).verifyForUser(USER_ID));
        assertEquals("ACTIVE", mapper.candidate(100L).getStatus());

        assertEquals(1, service(mapper, clock("2028-03-08T00:00:00Z")).verifyForUser(USER_ID));
        FixedExpenseCandidate buffered = mapper.candidate(100L);
        assertEquals("BUFFER", buffered.getStatus());
        assertEquals(LocalDate.of(2028, 3, 29), buffered.getNextExpectedDate());
    }

    @Test
    void advancesNextExpectedDateWithCalendarMonthArithmeticAtMonthEnd() {
        RecordingLifecycleMapper mapper = new RecordingLifecycleMapper();
        mapper.put(candidate(100L, USER_ID, "월말서비스", "ACTIVE", "2027-01-31",
                LocalDateTime.of(2026, 12, 31, 9, 0), false));
        mapper.payments.add(payment(501L, USER_ID, "월말서비스", "2027-01-31", "10000"));

        assertEquals(1, service(mapper, clock("2027-01-31T00:00:00Z")).verifyForUser(USER_ID));
        assertEquals("ACTIVE", mapper.candidate(100L).getStatus());
        assertEquals(LocalDate.of(2027, 2, 28), mapper.candidate(100L).getNextExpectedDate());
    }

    @Test
    void rerunningOrConcurrentlyRunningBatchAppliesBufferTransitionOnlyOnce() throws Exception {
        RecordingLifecycleMapper repeatedMapper = new RecordingLifecycleMapper();
        repeatedMapper.put(candidate(100L, USER_ID, "재실행서비스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));
        FixedExpenseLifecycleService repeatedService = service(repeatedMapper, clock("2026-06-23T00:00:00Z"));

        assertEquals(1, repeatedService.verifyForUser(USER_ID));
        assertEquals(0, repeatedService.verifyForUser(USER_ID));
        assertEquals(1, repeatedMapper.bufferTransitionCount);

        RecordingLifecycleMapper concurrentMapper = new RecordingLifecycleMapper(new CyclicBarrier(2));
        concurrentMapper.put(candidate(200L, USER_ID, "동시실행서비스", "ACTIVE", "2026-06-15",
                LocalDateTime.of(2026, 5, 15, 9, 0), false));
        FixedExpenseLifecycleService concurrentService = service(
                concurrentMapper, clock("2026-06-23T00:00:00Z"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentService.verifyForUser(USER_ID));
            Future<Integer> second = executor.submit(() -> concurrentService.verifyForUser(USER_ID));

            assertEquals(1, first.get(5, TimeUnit.SECONDS) + second.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals("BUFFER", concurrentMapper.candidate(200L).getStatus());
        assertEquals(1, concurrentMapper.bufferTransitionCount);
    }

    private FixedExpenseLifecycleService service(RecordingLifecycleMapper lifecycleMapper, Clock clock) {
        FixedExpenseDetectionMapper detectionMapper = mock(FixedExpenseDetectionMapper.class);
        when(detectionMapper.findActiveRule()).thenReturn(rule());
        return new FixedExpenseLifecycleService(lifecycleMapper, detectionMapper, clock);
    }

    private FixedExpenseDetectionRule rule() {
        return FixedExpenseDetectionRule.builder()
                .minRepeatCount(3)
                .amountTolerancePct(new BigDecimal("10.00"))
                .cycleToleranceDays(7)
                .build();
    }

    private Clock clock(String instant) {
        return Clock.fixed(Instant.parse(instant), SEOUL);
    }

    private FixedExpenseCandidate candidate(long id, long userId, String merchant, String status,
                                            String expectedDate, LocalDateTime confirmedAt, boolean excluded) {
        return FixedExpenseCandidate.builder()
                .id(id)
                .userId(userId)
                .merchantNameNormalized(merchant)
                .avgAmount(new BigDecimal("10000.00"))
                .cycleDays(30)
                .detectedCount(5)
                .isExcluded(excluded)
                .status(status)
                .nextExpectedDate(LocalDate.parse(expectedDate))
                .confirmedAt(confirmedAt)
                .build();
    }

    private FixedExpenseDetectionTransaction payment(long id, long userId, String merchant,
                                                      String date, String amount) {
        return FixedExpenseDetectionTransaction.builder()
                .id(id)
                .userId(userId)
                .merchantNameNormalized(merchant)
                .amount(new BigDecimal(amount))
                .trDate(LocalDate.parse(date))
                .classification("CONSUMPTION")
                .build();
    }

    private static class RecordingLifecycleMapper implements FixedExpenseLifecycleMapper {

        private final Map<Long, FixedExpenseCandidate> candidates = new HashMap<>();
        private final List<FixedExpenseDetectionTransaction> payments = new ArrayList<>();
        private final List<Long> linkedTransactionIds = new ArrayList<>();
        private final Map<Long, LocalDateTime> verifiedAtByCandidateId = new HashMap<>();
        private final CyclicBarrier readBarrier;
        private int bufferTransitionCount;

        private RecordingLifecycleMapper() {
            this(null);
        }

        private RecordingLifecycleMapper(CyclicBarrier readBarrier) {
            this.readBarrier = readBarrier;
        }

        @Override
        public List<FixedExpenseCandidate> findLifecycleCandidates(long userId) {
            List<FixedExpenseCandidate> snapshot;
            synchronized (this) {
                snapshot = candidates.values().stream()
                        .filter(candidate -> candidate.getUserId() == userId)
                        .filter(candidate -> !candidate.isExcluded())
                        .filter(candidate -> ("ACTIVE".equals(candidate.getStatus())
                                && candidate.getConfirmedAt() != null)
                                || "BUFFER".equals(candidate.getStatus()))
                        .sorted(Comparator.comparing(FixedExpenseCandidate::getId))
                        .toList();
            }
            if (readBarrier != null) {
                try {
                    readBarrier.await(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
            return snapshot;
        }

        @Override
        public synchronized List<FixedExpenseDetectionTransaction> findEligiblePayments(
                long userId, String merchantNameNormalized, LocalDate startDate, LocalDate endDate) {
            return payments.stream()
                    .filter(payment -> payment.getUserId() == userId)
                    .filter(payment -> merchantNameNormalized.equals(payment.getMerchantNameNormalized()))
                    .filter(payment -> !payment.getTrDate().isBefore(startDate)
                            && !payment.getTrDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public synchronized int advanceConfirmedCandidate(long candidateId, long userId,
                                                          LocalDate expectedDate, LocalDate nextExpectedDate) {
            FixedExpenseCandidate candidate = candidates.get(candidateId);
            if (!matches(candidate, userId, "ACTIVE", expectedDate) || candidate.getConfirmedAt() == null) {
                return 0;
            }
            candidates.put(candidateId, copy(candidate, "ACTIVE", nextExpectedDate,
                    candidate.getConfirmedAt(), candidate.getRelapseDetectedAt()));
            return 1;
        }

        @Override
        public synchronized int moveConfirmedCandidateToBuffer(long candidateId, long userId,
                                                               LocalDate expectedDate, LocalDate nextExpectedDate) {
            FixedExpenseCandidate candidate = candidates.get(candidateId);
            if (!matches(candidate, userId, "ACTIVE", expectedDate) || candidate.getConfirmedAt() == null) {
                return 0;
            }
            candidates.put(candidateId, copy(candidate, "BUFFER", nextExpectedDate,
                    candidate.getConfirmedAt(), candidate.getRelapseDetectedAt()));
            bufferTransitionCount++;
            return 1;
        }

        @Override
        public synchronized int reactivateBufferedCandidate(long candidateId, long userId,
                                                            LocalDate expectedDate, LocalDate nextExpectedDate,
                                                            LocalDateTime relapseDetectedAt) {
            FixedExpenseCandidate candidate = candidates.get(candidateId);
            if (!matches(candidate, userId, "BUFFER", expectedDate)) {
                return 0;
            }
            candidates.put(candidateId, copy(candidate, "ACTIVE", nextExpectedDate, null, relapseDetectedAt));
            return 1;
        }

        @Override
        public synchronized int verifyBufferedCandidate(long candidateId, long userId,
                                                        LocalDate expectedDate, LocalDateTime verifiedAt) {
            FixedExpenseCandidate candidate = candidates.get(candidateId);
            if (!matches(candidate, userId, "BUFFER", expectedDate)) {
                return 0;
            }
            candidates.put(candidateId, copy(candidate, "VERIFIED_CANCELLED", expectedDate,
                    candidate.getConfirmedAt(), candidate.getRelapseDetectedAt()));
            verifiedAtByCandidateId.put(candidateId, verifiedAt);
            return 1;
        }

        @Override
        public synchronized int linkPaymentToCandidate(long transactionId, long userId, long candidateId) {
            boolean owned = payments.stream().anyMatch(payment -> payment.getId() == transactionId
                    && payment.getUserId() == userId);
            if (!owned) {
                return 0;
            }
            linkedTransactionIds.add(transactionId);
            return 1;
        }

        private synchronized void put(FixedExpenseCandidate candidate) {
            candidates.put(candidate.getId(), candidate);
        }

        private synchronized FixedExpenseCandidate candidate(long candidateId) {
            return candidates.get(candidateId);
        }

        private boolean matches(FixedExpenseCandidate candidate, long userId,
                                String expectedStatus, LocalDate expectedDate) {
            return candidate != null
                    && candidate.getUserId() == userId
                    && !candidate.isExcluded()
                    && expectedStatus.equals(candidate.getStatus())
                    && expectedDate.equals(candidate.getNextExpectedDate());
        }

        private FixedExpenseCandidate copy(FixedExpenseCandidate source, String status,
                                           LocalDate nextExpectedDate, LocalDateTime confirmedAt,
                                           LocalDateTime relapseDetectedAt) {
            return FixedExpenseCandidate.builder()
                    .id(source.getId())
                    .userId(source.getUserId())
                    .merchantNameNormalized(source.getMerchantNameNormalized())
                    .avgAmount(source.getAvgAmount())
                    .cycleDays(source.getCycleDays())
                    .detectedCount(source.getDetectedCount())
                    .categoryId(source.getCategoryId())
                    .isExcluded(source.isExcluded())
                    .status(status)
                    .nextExpectedDate(nextExpectedDate)
                    .confirmedAt(confirmedAt)
                    .relapseDetectedAt(relapseDetectedAt)
                    .build();
        }
    }
}

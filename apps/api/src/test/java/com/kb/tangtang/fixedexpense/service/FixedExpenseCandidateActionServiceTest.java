package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionResponseDto;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseCandidateActionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseCandidateActionServiceTest {

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final long CANDIDATE_ID = 101L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T01:02:03Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime DECIDED_AT = LocalDateTime.of(2026, 8, 14, 10, 2, 3);

    @Test
    @DisplayName("활성 미확정 후보를 확정하면 confirmed_at을 현재 시각으로 기록한다")
    void confirmsActiveUnconfirmedCandidate() {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, USER_ID, "ACTIVE", false, null));

        FixedExpenseCandidateActionResponseDto result = service(mapper)
                .decide(USER_ID, CANDIDATE_ID, "CONFIRM");

        assertEquals(CANDIDATE_ID, result.getCandidateId());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(false, result.getIsExcluded());
        assertEquals(DECIDED_AT, result.getConfirmedAt());
        assertEquals(DECIDED_AT, mapper.candidate(CANDIDATE_ID).getConfirmedAt());
        assertFalse(mapper.candidate(CANDIDATE_ID).isExcluded());
    }

    @Test
    @DisplayName("활성 미확정 후보를 제외하면 재노출 억제 상태를 기록한다")
    void excludesActiveUnconfirmedCandidate() {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, USER_ID, "ACTIVE", false, null));

        FixedExpenseCandidateActionResponseDto result = service(mapper)
                .decide(USER_ID, CANDIDATE_ID, "EXCLUDE");

        assertEquals("ACTIVE", result.getStatus());
        assertEquals(true, result.getIsExcluded());
        assertNull(result.getConfirmedAt());
        assertTrue(mapper.candidate(CANDIDATE_ID).isExcluded());
        assertNull(mapper.candidate(CANDIDATE_ID).getConfirmedAt());
    }

    @Test
    @DisplayName("없는 후보와 타인 후보는 모두 NOT_FOUND로 처리한다")
    void hidesCandidateOwnership() {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, OTHER_USER_ID, "ACTIVE", false, null));

        BusinessException otherUser = assertThrows(BusinessException.class,
                () -> service(mapper).decide(USER_ID, CANDIDATE_ID, "CONFIRM"));
        BusinessException missing = assertThrows(BusinessException.class,
                () -> service(mapper).decide(USER_ID, 999L, "CONFIRM"));

        assertEquals("NOT_FOUND", otherUser.getCode());
        assertEquals(HttpStatus.NOT_FOUND, otherUser.getHttpStatus());
        assertEquals("NOT_FOUND", missing.getCode());
        assertEquals(HttpStatus.NOT_FOUND, missing.getHttpStatus());
    }

    @Test
    @DisplayName("지원하지 않는 action은 상태를 바꾸지 않고 INVALID_REQUEST를 반환한다")
    void rejectsInvalidAction() {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, USER_ID, "ACTIVE", false, null));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service(mapper).decide(USER_ID, CANDIDATE_ID, "DISMISS"));

        assertEquals("INVALID_REQUEST", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertNull(mapper.candidate(CANDIDATE_ID).getConfirmedAt());
        assertFalse(mapper.candidate(CANDIDATE_ID).isExcluded());
    }

    @Test
    @DisplayName("같은 action 재전송은 200 상태를 돌려주고 반대 action과 비활성 상태는 409이다")
    void handlesRetriesAndConflictingTransitions() {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, USER_ID, "ACTIVE", false, null));
        FixedExpenseCandidateActionService service = service(mapper);

        FixedExpenseCandidateActionResponseDto first = service.decide(USER_ID, CANDIDATE_ID, "CONFIRM");
        FixedExpenseCandidateActionResponseDto retried = service.decide(USER_ID, CANDIDATE_ID, "CONFIRM");
        BusinessException opposite = assertThrows(BusinessException.class,
                () -> service.decide(USER_ID, CANDIDATE_ID, "EXCLUDE"));

        assertEquals(first.getConfirmedAt(), retried.getConfirmedAt());
        assertEquals(HttpStatus.CONFLICT, opposite.getHttpStatus());
        assertEquals("INVALID_REQUEST", opposite.getCode());

        mapper.put(candidate(CANDIDATE_ID, USER_ID, "BUFFER", false, null));
        BusinessException inactive = assertThrows(BusinessException.class,
                () -> service.decide(USER_ID, CANDIDATE_ID, "CONFIRM"));
        assertEquals(HttpStatus.CONFLICT, inactive.getHttpStatus());
    }

    @Test
    @DisplayName("동시 확정·제외 요청은 하나만 반영돼 confirmed_at과 제외 상태가 모순되지 않는다")
    void keepsStateConsistentForConcurrentConflictingRequests() throws Exception {
        RecordingMapper mapper = mapperWith(candidate(CANDIDATE_ID, USER_ID, "ACTIVE", false, null));
        FixedExpenseCandidateActionService service = service(mapper);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<DecisionOutcome> confirm = executor.submit(
                    () -> decideAfterStart(service, ready, start, "CONFIRM"));
            Future<DecisionOutcome> exclude = executor.submit(
                    () -> decideAfterStart(service, ready, start, "EXCLUDE"));

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            List<DecisionOutcome> outcomes = List.of(
                    confirm.get(2, TimeUnit.SECONDS),
                    exclude.get(2, TimeUnit.SECONDS));
            long successCount = outcomes.stream().filter(outcome -> outcome.result() != null).count();
            long conflictCount = outcomes.stream()
                    .filter(outcome -> outcome.exception() != null
                            && outcome.exception().getHttpStatus() == HttpStatus.CONFLICT)
                    .count();

            assertEquals(1, successCount);
            assertEquals(1, conflictCount);

            FixedExpenseCandidate persisted = mapper.candidate(CANDIDATE_ID);
            assertEquals("ACTIVE", persisted.getStatus());
            assertFalse(persisted.isExcluded() && persisted.getConfirmedAt() != null);
            assertTrue(persisted.isExcluded() || persisted.getConfirmedAt() != null);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("후보 결정은 Service 트랜잭션 경계에서 처리한다")
    void declaresTransactionAtServiceBoundary() throws Exception {
        Transactional transactional = FixedExpenseCandidateActionService.class
                .getDeclaredMethod("decide", long.class, long.class, String.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    private DecisionOutcome decideAfterStart(FixedExpenseCandidateActionService service,
                                             CountDownLatch ready,
                                             CountDownLatch start,
                                             String action) throws InterruptedException {
        ready.countDown();
        if (!start.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 신호를 받지 못했습니다.");
        }
        try {
            return new DecisionOutcome(service.decide(USER_ID, CANDIDATE_ID, action), null);
        } catch (BusinessException exception) {
            return new DecisionOutcome(null, exception);
        }
    }

    private FixedExpenseCandidateActionService service(RecordingMapper mapper) {
        return new FixedExpenseCandidateActionService(mapper, CLOCK);
    }

    private RecordingMapper mapperWith(FixedExpenseCandidate... candidates) {
        RecordingMapper mapper = new RecordingMapper();
        for (FixedExpenseCandidate candidate : candidates) {
            mapper.put(candidate);
        }
        return mapper;
    }

    private FixedExpenseCandidate candidate(long id, long userId, String status,
                                            boolean excluded, LocalDateTime confirmedAt) {
        return FixedExpenseCandidate.builder()
                .id(id)
                .userId(userId)
                .status(status)
                .isExcluded(excluded)
                .confirmedAt(confirmedAt)
                .build();
    }

    private record DecisionOutcome(FixedExpenseCandidateActionResponseDto result,
                                   BusinessException exception) {
    }

    private static class RecordingMapper implements FixedExpenseCandidateActionMapper {

        private final Map<Long, FixedExpenseCandidate> candidates = new HashMap<>();

        @Override
        public synchronized FixedExpenseCandidate findOwnedCandidateForUpdate(long candidateId, long userId) {
            FixedExpenseCandidate candidate = candidates.get(candidateId);
            if (candidate == null || candidate.getUserId() == null || candidate.getUserId() != userId) {
                return null;
            }
            return candidate;
        }

        @Override
        public synchronized int confirmCandidate(long candidateId, long userId, LocalDateTime confirmedAt) {
            FixedExpenseCandidate candidate = findOwnedCandidateForUpdate(candidateId, userId);
            if (!isActionable(candidate)) {
                return 0;
            }
            put(copy(candidate, false, confirmedAt));
            return 1;
        }

        @Override
        public synchronized int excludeCandidate(long candidateId, long userId) {
            FixedExpenseCandidate candidate = findOwnedCandidateForUpdate(candidateId, userId);
            if (!isActionable(candidate)) {
                return 0;
            }
            put(copy(candidate, true, null));
            return 1;
        }

        private boolean isActionable(FixedExpenseCandidate candidate) {
            return candidate != null
                    && "ACTIVE".equals(candidate.getStatus())
                    && !candidate.isExcluded()
                    && candidate.getConfirmedAt() == null;
        }

        private FixedExpenseCandidate copy(FixedExpenseCandidate source,
                                           boolean excluded,
                                           LocalDateTime confirmedAt) {
            return FixedExpenseCandidate.builder()
                    .id(source.getId())
                    .userId(source.getUserId())
                    .status(source.getStatus())
                    .isExcluded(excluded)
                    .confirmedAt(confirmedAt)
                    .build();
        }

        private synchronized void put(FixedExpenseCandidate candidate) {
            candidates.put(candidate.getId(), candidate);
        }

        private synchronized FixedExpenseCandidate candidate(long candidateId) {
            return candidates.get(candidateId);
        }
    }
}

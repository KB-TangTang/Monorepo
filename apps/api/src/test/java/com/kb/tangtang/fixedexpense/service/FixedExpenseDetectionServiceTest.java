package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionRule;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FixedExpenseDetectionServiceTest {

    private static final long USER_ID = 7L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock AUGUST_CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"), SEOUL);

    @Test
    void createsCandidateFromThreeDifferentMonthlyPayments() {
        RecordingMapper mapper = mapperWith(
                transaction("넷플릭스", "2026-05-15", "9000"),
                transaction("넷플릭스", "2026-06-15", "10000"),
                transaction("넷플릭스", "2026-07-15", "11000"));

        int detected = service(mapper, AUGUST_CLOCK).detectForUser(USER_ID);

        FixedExpenseCandidate candidate = mapper.candidate("넷플릭스");
        assertEquals(1, detected);
        assertEquals(3, candidate.getDetectedCount());
        assertEquals(0, new BigDecimal("10000.00").compareTo(candidate.getAvgAmount()));
        assertEquals(LocalDate.of(2026, 8, 15), candidate.getNextExpectedDate());
        assertEquals(1, mapper.linkedCandidateIds.size());
    }

    @Test
    void rejectsFewerThanThreeMonthsAndOtherUsers() {
        RecordingMapper mapper = mapperWith(
                transaction("두달서비스", "2026-06-15", "10000"),
                transaction("두달서비스", "2026-07-15", "10000"),
                transactionFor(99L, "다른사용자", "2026-05-15", "10000"),
                transactionFor(USER_ID, "다른사용자", "2026-05-15", "10000"),
                transactionFor(USER_ID, "다른사용자", "2026-06-15", "10000"),
                transactionFor(USER_ID, "다른사용자", "2026-07-15", "10000"));

        int detected = service(mapper, AUGUST_CLOCK).detectForUser(USER_ID);

        assertEquals(1, detected);
        assertNull(mapper.candidate("두달서비스"));
        assertEquals(3, mapper.candidate("다른사용자").getDetectedCount());
    }

    @Test
    void ignoresCancelledRefundTransferAndExcludedTransactions() {
        RecordingMapper mapper = mapperWith(
                invalidTransaction("취소서비스", "2026-05-15", "10000", "CONSUMPTION", "Y", false, false),
                invalidTransaction("취소서비스", "2026-06-15", "10000", "CONSUMPTION", "Y", false, false),
                invalidTransaction("취소서비스", "2026-07-15", "10000", "CONSUMPTION", "Y", false, false),
                invalidTransaction("환불서비스", "2026-05-15", "10000", "CONSUMPTION", null, true, false),
                invalidTransaction("환불서비스", "2026-06-15", "10000", "CONSUMPTION", null, true, false),
                invalidTransaction("환불서비스", "2026-07-15", "10000", "CONSUMPTION", null, true, false),
                invalidTransaction("이체서비스", "2026-05-15", "10000", "TRANSFER", null, false, false),
                invalidTransaction("이체서비스", "2026-06-15", "10000", "TRANSFER", null, false, false),
                invalidTransaction("이체서비스", "2026-07-15", "10000", "TRANSFER", null, false, false),
                invalidTransaction("제외서비스", "2026-05-15", "10000", "CONSUMPTION", null, false, true),
                invalidTransaction("제외서비스", "2026-06-15", "10000", "CONSUMPTION", null, false, true),
                invalidTransaction("제외서비스", "2026-07-15", "10000", "CONSUMPTION", null, false, true),
                transaction("정상서비스", "2026-05-15", "10000"),
                transaction("정상서비스", "2026-06-15", "10000"),
                transaction("정상서비스", "2026-07-15", "10000"));

        int detected = service(mapper, AUGUST_CLOCK).detectForUser(USER_ID);

        assertEquals(1, detected);
        assertNull(mapper.candidate("취소서비스"));
        assertNull(mapper.candidate("환불서비스"));
        assertNull(mapper.candidate("이체서비스"));
        assertNull(mapper.candidate("제외서비스"));
        assertEquals(3, mapper.candidate("정상서비스").getDetectedCount());
    }

    @Test
    void acceptsDateAndAmountToleranceBoundariesButRejectsOutsideValues() {
        RecordingMapper dateBoundary = mapperWith(
                transaction("날짜경계", "2026-05-15", "10000"),
                transaction("날짜경계", "2026-06-22", "10000"),
                transaction("날짜경계", "2026-07-22", "10000"));
        RecordingMapper amountBoundary = mapperWith(
                transaction("금액경계", "2026-05-15", "9000"),
                transaction("금액경계", "2026-06-15", "10000"),
                transaction("금액경계", "2026-07-15", "11000"));
        RecordingMapper outsideBoundary = mapperWith(
                transaction("초과금액", "2026-05-15", "8900"),
                transaction("초과금액", "2026-06-15", "10000"),
                transaction("초과금액", "2026-07-15", "11100"));

        assertEquals(1, service(dateBoundary, AUGUST_CLOCK).detectForUser(USER_ID));
        assertEquals(1, service(amountBoundary, AUGUST_CLOCK).detectForUser(USER_ID));
        assertEquals(0, service(outsideBoundary, AUGUST_CLOCK).detectForUser(USER_ID));
    }

    @Test
    void rerunningBatchKeepsOneCandidateAndDoesNotIncreaseDetectedCount() {
        RecordingMapper mapper = mapperWith(
                transaction("재실행서비스", "2026-05-15", "10000"),
                transaction("재실행서비스", "2026-06-15", "10000"),
                transaction("재실행서비스", "2026-07-15", "10000"));
        FixedExpenseDetectionService service = service(mapper, AUGUST_CLOCK);

        service.detectForUser(USER_ID);
        service.detectForUser(USER_ID);

        assertEquals(1, mapper.candidates.size());
        assertEquals(3, mapper.candidate("재실행서비스").getDetectedCount());
        assertEquals(2, mapper.upsertCount);
    }

    @Test
    void preservesConfirmedCandidateAndReactivatesBufferAsUnconfirmed() {
        RecordingMapper confirmedMapper = mapperWith(
                transaction("확정서비스", "2026-05-15", "10000"),
                transaction("확정서비스", "2026-06-15", "10000"),
                transaction("확정서비스", "2026-07-15", "10000"));
        confirmedMapper.putCandidate(candidate("확정서비스", "ACTIVE",
                LocalDateTime.of(2026, 8, 1, 9, 0), null));

        RecordingMapper bufferMapper = mapperWith(
                transaction("버퍼서비스", "2026-05-15", "10000"),
                transaction("버퍼서비스", "2026-06-15", "10000"),
                transaction("버퍼서비스", "2026-07-15", "10000"));
        bufferMapper.putCandidate(candidate("버퍼서비스", "BUFFER",
                LocalDateTime.of(2026, 8, 1, 9, 0), null));

        service(confirmedMapper, AUGUST_CLOCK).detectForUser(USER_ID);
        service(bufferMapper, AUGUST_CLOCK).detectForUser(USER_ID);

        FixedExpenseCandidate confirmed = confirmedMapper.candidate("확정서비스");
        assertEquals("ACTIVE", confirmed.getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), confirmed.getConfirmedAt());

        FixedExpenseCandidate reactivated = bufferMapper.candidate("버퍼서비스");
        assertEquals("ACTIVE", reactivated.getStatus());
        assertNull(reactivated.getConfirmedAt());
        assertEquals(LocalDateTime.of(2026, 8, 14, 9, 0), reactivated.getRelapseDetectedAt());

        RecordingMapper excludedMapper = mapperWith(
                transaction("제외후보", "2026-05-15", "10000"),
                transaction("제외후보", "2026-06-15", "10000"),
                transaction("제외후보", "2026-07-15", "10000"));
        excludedMapper.putCandidate(FixedExpenseCandidate.builder()
                .id(99L)
                .userId(USER_ID)
                .merchantNameNormalized("제외후보")
                .avgAmount(new BigDecimal("10000.00"))
                .cycleDays(30)
                .detectedCount(3)
                .isExcluded(true)
                .status("ACTIVE")
                .build());

        assertEquals(0, service(excludedMapper, AUGUST_CLOCK).detectForUser(USER_ID));
        assertEquals(0, excludedMapper.linkedCandidateIds.size());
        assertEquals(true, excludedMapper.candidate("제외후보").isExcluded());
    }

    @Test
    void keepsVerifiedCancelledStatusWhenAHistoricalPatternAppearsAgain() {
        RecordingMapper mapper = mapperWith(
                transaction("해지서비스", "2026-05-15", "10000"),
                transaction("해지서비스", "2026-06-15", "10000"),
                transaction("해지서비스", "2026-07-15", "10000"));
        mapper.putCandidate(candidate("해지서비스", "VERIFIED_CANCELLED", null, null));

        service(mapper, AUGUST_CLOCK).detectForUser(USER_ID);

        assertEquals("VERIFIED_CANCELLED", mapper.candidate("해지서비스").getStatus());
        assertEquals(1, mapper.linkedCandidateIds.size());
    }

    @Test
    void calculatesNextPaymentWithCalendarMonthArithmeticAtLeapMonthEnd() {
        Clock leapYearClock = Clock.fixed(Instant.parse("2028-03-01T00:00:00Z"), SEOUL);
        RecordingMapper mapper = mapperWith(
                transaction("윤년서비스", "2027-12-31", "10000"),
                transaction("윤년서비스", "2028-01-31", "10000"),
                transaction("윤년서비스", "2028-02-29", "10000"));

        service(mapper, leapYearClock).detectForUser(USER_ID);

        assertEquals(LocalDate.of(2028, 3, 29),
                mapper.candidate("윤년서비스").getNextExpectedDate());
    }

    private FixedExpenseDetectionService service(RecordingMapper mapper, Clock clock) {
        return new FixedExpenseDetectionService(mapper, clock);
    }

    private RecordingMapper mapperWith(FixedExpenseDetectionTransaction... transactions) {
        RecordingMapper mapper = new RecordingMapper(rule());
        mapper.transactions.addAll(List.of(transactions));
        return mapper;
    }

    private FixedExpenseDetectionRule rule() {
        return FixedExpenseDetectionRule.builder()
                .minRepeatCount(3)
                .amountTolerancePct(new BigDecimal("10.00"))
                .cycleToleranceDays(7)
                .build();
    }

    private FixedExpenseDetectionTransaction transaction(String merchant, String date,
                                                         String amount) {
        return transactionFor(USER_ID, merchant, date, amount);
    }

    private FixedExpenseDetectionTransaction transactionFor(long userId, String merchant,
                                                             String date, String amount) {
        return FixedExpenseDetectionTransaction.builder()
                .id((long) (transactionsSeed++))
                .userId(userId)
                .merchantNameNormalized(merchant)
                .amount(new BigDecimal(amount))
                .trDate(LocalDate.parse(date))
                .classification("CONSUMPTION")
                .build();
    }

    private FixedExpenseDetectionTransaction invalidTransaction(String merchant, String date,
                                                                 String amount, String classification,
                                                                 String cancelYn, boolean isRefund,
                                                                 boolean isExcludedFromSummary) {
        return FixedExpenseDetectionTransaction.builder()
                .id((long) (transactionsSeed++))
                .userId(USER_ID)
                .merchantNameNormalized(merchant)
                .amount(new BigDecimal(amount))
                .trDate(LocalDate.parse(date))
                .classification(classification)
                .cancelYn(cancelYn)
                .isRefund(isRefund)
                .isExcludedFromSummary(isExcludedFromSummary)
                .build();
    }

    private int transactionsSeed = 1;

    private FixedExpenseCandidate candidate(String merchant, String status,
                                            LocalDateTime confirmedAt,
                                            LocalDateTime relapseDetectedAt) {
        return FixedExpenseCandidate.builder()
                .id((long) (candidateSeed++))
                .userId(USER_ID)
                .merchantNameNormalized(merchant)
                .avgAmount(new BigDecimal("10000.00"))
                .cycleDays(30)
                .detectedCount(3)
                .status(status)
                .confirmedAt(confirmedAt)
                .relapseDetectedAt(relapseDetectedAt)
                .nextExpectedDate(LocalDate.of(2026, 8, 15))
                .build();
    }

    private int candidateSeed = 100;

    private static class RecordingMapper implements FixedExpenseDetectionMapper {

        private final FixedExpenseDetectionRule rule;
        private final List<FixedExpenseDetectionTransaction> transactions = new ArrayList<>();
        private final Map<String, FixedExpenseCandidate> candidates = new HashMap<>();
        private final List<Long> linkedCandidateIds = new ArrayList<>();
        private int upsertCount;
        private long nextCandidateId = 1L;

        private RecordingMapper(FixedExpenseDetectionRule rule) {
            this.rule = rule;
        }

        @Override
        public FixedExpenseDetectionRule findActiveRule() {
            return rule;
        }

        @Override
        public List<Long> findActiveUserIds() {
            return List.of(USER_ID);
        }

        @Override
        public List<FixedExpenseDetectionTransaction> findDetectionTransactions(
                long userId, LocalDate startDate, LocalDate endDate) {
            return transactions;
        }

        @Override
        public FixedExpenseCandidate findCandidate(long userId, String merchantNameNormalized) {
            return candidates.get(merchantNameNormalized);
        }

        @Override
        public int upsertCandidate(FixedExpenseCandidate candidate) {
            upsertCount++;
            candidates.computeIfAbsent(candidate.getMerchantNameNormalized(), key ->
                    copy(candidate, nextCandidateId++));
            return 1;
        }

        @Override
        public int updateDetectedCandidate(FixedExpenseCandidate incoming,
                                            boolean reactivateBuffer,
                                            LocalDateTime relapseDetectedAt) {
            FixedExpenseCandidate existing = candidates.get(incoming.getMerchantNameNormalized());
            if (existing == null || existing.isExcluded()) {
                return 0;
            }
            candidates.put(incoming.getMerchantNameNormalized(), FixedExpenseCandidate.builder()
                    .id(existing.getId())
                    .userId(existing.getUserId())
                    .merchantNameNormalized(existing.getMerchantNameNormalized())
                    .avgAmount(incoming.getAvgAmount())
                    .cycleDays(incoming.getCycleDays())
                    .detectedCount(incoming.getDetectedCount())
                    .categoryId(incoming.getCategoryId())
                    .isExcluded(existing.isExcluded())
                    .status(reactivateBuffer ? "ACTIVE" : existing.getStatus())
                    .nextExpectedDate(incoming.getNextExpectedDate())
                    .confirmedAt(reactivateBuffer ? null : existing.getConfirmedAt())
                    .relapseDetectedAt(reactivateBuffer
                            ? relapseDetectedAt : existing.getRelapseDetectedAt())
                    .build());
            return 1;
        }

        @Override
        public int linkTransactionsToCandidate(long userId, String merchantNameNormalized,
                                                LocalDate startDate, LocalDate endDate,
                                                long candidateId) {
            linkedCandidateIds.add(candidateId);
            return 1;
        }

        private void putCandidate(FixedExpenseCandidate candidate) {
            candidates.put(candidate.getMerchantNameNormalized(), candidate);
        }

        private FixedExpenseCandidate candidate(String merchant) {
            return candidates.get(merchant);
        }

        private FixedExpenseCandidate copy(FixedExpenseCandidate source, long id) {
            return FixedExpenseCandidate.builder()
                    .id(id)
                    .userId(source.getUserId())
                    .merchantNameNormalized(source.getMerchantNameNormalized())
                    .avgAmount(source.getAvgAmount())
                    .cycleDays(source.getCycleDays())
                    .detectedCount(source.getDetectedCount())
                    .categoryId(source.getCategoryId())
                    .isExcluded(source.isExcluded())
                    .status(source.getStatus())
                    .nextExpectedDate(source.getNextExpectedDate())
                    .confirmedAt(source.getConfirmedAt())
                    .relapseDetectedAt(source.getRelapseDetectedAt())
                    .build();
        }
    }
}

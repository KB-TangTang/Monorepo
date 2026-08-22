package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionRule;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 최근 6개월의 월간 반복 소비를 고정지출 후보로 저장한다. */
@Service
@Log4j2
public class FixedExpenseDetectionService {

    private static final int DETECTION_MONTH_COUNT = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_AMOUNT_TOLERANCE_PCT = BigDecimal.TEN;
    private static final String ACTIVE = "ACTIVE";

    private final FixedExpenseDetectionMapper mapper;
    private final Clock clock;

    @Autowired
    public FixedExpenseDetectionService(
            FixedExpenseDetectionMapper mapper,
            @Value("${fixed.expense.detection.zone:Asia/Seoul}") String zoneId) {
        this(mapper, Clock.system(ZoneId.of(zoneId)));
    }

    FixedExpenseDetectionService(FixedExpenseDetectionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 한 사용자의 탐지 결과를 하나의 트랜잭션으로 갱신한다. */
    @Transactional
    public int detectForUser(long userId) {
        FixedExpenseDetectionRule rule = mapper.findActiveRule();
        if (rule == null) {
            return 0;
        }

        DetectionWindow window = detectionWindow();
        List<FixedExpenseDetectionTransaction> transactions = mapper.findDetectionTransactions(
                userId, window.startDate(), window.endDate());
        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }

        Map<String, List<FixedExpenseDetectionTransaction>> byMerchant = new LinkedHashMap<>();
        for (FixedExpenseDetectionTransaction transaction : transactions) {
            if (!isEligibleTransaction(transaction, userId)) {
                continue;
            }
            byMerchant.computeIfAbsent(transaction.getMerchantNameNormalized(), key -> new ArrayList<>())
                    .add(transaction);
        }

        int detectedCount = 0;
        for (Map.Entry<String, List<FixedExpenseDetectionTransaction>> entry : byMerchant.entrySet()) {
            DetectedPattern pattern = detectPattern(entry.getValue(), rule);
            if (pattern == null) {
                continue;
            }

            FixedExpenseCandidate incoming = toCandidate(userId, entry.getKey(), pattern);
            mapper.upsertCandidate(incoming);
            FixedExpenseCandidate persisted = mapper.findCandidate(userId, entry.getKey());
            if (persisted == null || persisted.isExcluded() || !ACTIVE.equals(persisted.getStatus())) {
                continue;
            }

            mapper.updateDetectedCandidate(incomingWithId(incoming, persisted.getId()));
            mapper.linkTransactionsToCandidate(
                    userId,
                    entry.getKey(),
                    window.startDate(),
                    window.endDate(),
                    persisted.getId());
            detectedCount++;
        }
        return detectedCount;
    }

    private boolean isEligibleTransaction(FixedExpenseDetectionTransaction transaction, long userId) {
        return transaction != null
                && transaction.getUserId() != null
                && transaction.getUserId() == userId
                && transaction.getMerchantNameNormalized() != null
                && !transaction.getMerchantNameNormalized().isBlank()
                && "CONSUMPTION".equalsIgnoreCase(transaction.getClassification())
                && !"Y".equalsIgnoreCase(transaction.getCancelYn())
                && !transaction.isRefund()
                && !transaction.isExcludedFromSummary();
    }

    private DetectionWindow detectionWindow() {
        YearMonth currentMonth = YearMonth.now(clock);
        return new DetectionWindow(
                currentMonth.minusMonths(DETECTION_MONTH_COUNT - 1L).atDay(1),
                currentMonth.plusMonths(1).atDay(1));
    }

    private DetectedPattern detectPattern(List<FixedExpenseDetectionTransaction> transactions,
                                           FixedExpenseDetectionRule rule) {
        Map<YearMonth, FixedExpenseDetectionTransaction> representativeByMonth = new LinkedHashMap<>();
        transactions.stream()
                .filter(transaction -> transaction.getTrDate() != null
                        && transaction.getAmount() != null
                        && transaction.getAmount().signum() > 0)
                .sorted(Comparator.comparing(FixedExpenseDetectionTransaction::getTrDate)
                        .thenComparing(FixedExpenseDetectionTransaction::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(transaction -> representativeByMonth.put(
                        YearMonth.from(transaction.getTrDate()), transaction));

        List<FixedExpenseDetectionTransaction> representatives = new ArrayList<>(
                representativeByMonth.values());
        representatives.sort(Comparator.comparing(FixedExpenseDetectionTransaction::getTrDate));
        if (representatives.size() < rule.getMinRepeatCount()) {
            return null;
        }

        FixedExpenseDetectionTransaction latest = representatives.get(representatives.size() - 1);
        int anchorDay = latest.getTrDate().getDayOfMonth();
        int dateTolerance = Math.max(0, rule.getCycleToleranceDays());
        for (FixedExpenseDetectionTransaction transaction : representatives) {
            if (Math.abs(transaction.getTrDate().getDayOfMonth() - anchorDay) > dateTolerance) {
                return null;
            }
        }

        BigDecimal averageAmount = representatives.stream()
                .map(FixedExpenseDetectionTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(representatives.size()), 2, RoundingMode.HALF_UP);
        BigDecimal amountTolerancePct = rule.getAmountTolerancePct() == null
                ? DEFAULT_AMOUNT_TOLERANCE_PCT
                : rule.getAmountTolerancePct();
        BigDecimal allowedAmountDifference = averageAmount
                .multiply(amountTolerancePct)
                .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
        for (FixedExpenseDetectionTransaction transaction : representatives) {
            if (transaction.getAmount().subtract(averageAmount).abs()
                    .compareTo(allowedAmountDifference) > 0) {
                return null;
            }
        }

        long intervalTotal = 0;
        for (int index = 1; index < representatives.size(); index++) {
            intervalTotal += ChronoUnit.DAYS.between(
                    representatives.get(index - 1).getTrDate(),
                    representatives.get(index).getTrDate());
        }
        int cycleDays = BigDecimal.valueOf(intervalTotal)
                .divide(BigDecimal.valueOf(representatives.size() - 1), 0, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE)
                .intValue();

        return new DetectedPattern(
                averageAmount,
                cycleDays,
                representatives.size(),
                latest.getCategoryId(),
                latest.getTrDate(),
                latest.getTrDate().plusMonths(1));
    }

    private FixedExpenseCandidate toCandidate(long userId, String merchantNameNormalized,
                                               DetectedPattern pattern) {
        return FixedExpenseCandidate.builder()
                .userId(userId)
                .merchantNameNormalized(merchantNameNormalized)
                .avgAmount(pattern.averageAmount())
                .cycleDays(pattern.cycleDays())
                .detectedCount(pattern.detectedCount())
                .categoryId(pattern.categoryId())
                .status(ACTIVE)
                .nextExpectedDate(pattern.nextExpectedDate())
                .build();
    }

    private FixedExpenseCandidate incomingWithId(FixedExpenseCandidate candidate, Long id) {
        return FixedExpenseCandidate.builder()
                .id(id)
                .userId(candidate.getUserId())
                .merchantNameNormalized(candidate.getMerchantNameNormalized())
                .avgAmount(candidate.getAvgAmount())
                .cycleDays(candidate.getCycleDays())
                .detectedCount(candidate.getDetectedCount())
                .categoryId(candidate.getCategoryId())
                .status(candidate.getStatus())
                .nextExpectedDate(candidate.getNextExpectedDate())
                .build();
    }

    private record DetectionWindow(LocalDate startDate, LocalDate endDate) {
    }

    private record DetectedPattern(BigDecimal averageAmount,
                                   int cycleDays,
                                   int detectedCount,
                                   Long categoryId,
                                   LocalDate lastPaymentDate,
                                   LocalDate nextExpectedDate) {
    }
}

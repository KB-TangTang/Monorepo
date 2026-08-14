package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseReadRow;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseBillingCycleDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseChangeNoticeDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseDetailDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseEvidenceMonthDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseItemDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewSummaryDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpensePaymentHistoryDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsItemDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsReportDto;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseQueryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** 고정지출의 현재 활성 상태를 목록·상세·절약 감정서로 조회한다. */
@Service
@Transactional(readOnly = true)
public class FixedExpenseQueryService {

    private static final String ACTIVE = "ACTIVE";
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final FixedExpenseQueryMapper mapper;
    private final Clock clock;

    @Autowired
    public FixedExpenseQueryService(
            FixedExpenseQueryMapper mapper,
            @Value("${fixed.expense.detection.zone:Asia/Seoul}") String zoneId) {
        this(mapper, Clock.system(ZoneId.of(zoneId)));
    }

    FixedExpenseQueryService(FixedExpenseQueryMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public FixedExpenseOverviewDto getOverview(long userId, String rawYearMonth, Long categoryId) {
        YearMonth yearMonth = resolveCurrentYearMonth(rawYearMonth);
        validateCategoryId(categoryId);

        List<FixedExpenseItemDto> confirmed = toItems(
                mapper.findActiveItems(userId, categoryId, true), true);
        List<FixedExpenseItemDto> candidates = toItems(
                mapper.findActiveItems(userId, categoryId, false), false);

        BigDecimal expectedMonthlyAmount = sumAverageAmount(confirmed)
                .add(sumAverageAmount(candidates));
        return FixedExpenseOverviewDto.builder()
                .yearMonth(yearMonth.toString())
                .summary(FixedExpenseOverviewSummaryDto.builder()
                        .expectedMonthlyAmount(expectedMonthlyAmount)
                        .confirmedCount(confirmed.size())
                        .candidateCount(candidates.size())
                        .build())
                .confirmed(confirmed)
                .candidates(candidates)
                .build();
    }

    public FixedExpenseDetailDto getDetail(long userId, long candidateId) {
        if (candidateId <= 0) {
            throw new BusinessException("INVALID_REQUEST", "candidateId는 양수여야 합니다.");
        }

        FixedExpenseReadRow row = mapper.findOwnedActiveItem(userId, candidateId);
        if (row == null) {
            throw new BusinessException("NOT_FOUND", "고정지출 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        YearMonth currentYearMonth = YearMonth.now(clock);
        LocalDate historyStartDate = currentYearMonth.minusMonths(5).atDay(1);
        LocalDate historyEndDate = currentYearMonth.plusMonths(1).atDay(1);
        List<FixedExpensePaymentHistoryDto> history = mapper.findRecentPaymentHistory(
                userId, candidateId, historyStartDate, historyEndDate);
        if (history == null) {
            history = Collections.emptyList();
        }
        BigDecimal sixMonthTotal = mapper.sumPaymentHistory(
                userId, candidateId, historyStartDate, historyEndDate);

        return FixedExpenseDetailDto.builder()
                .item(toItem(row, row.getConfirmedAt() != null))
                .paymentHistory(history)
                .evidenceMonths(toEvidenceMonths(history, currentYearMonth))
                .sixMonthTotal(sixMonthTotal == null ? BigDecimal.ZERO : sixMonthTotal)
                .changeNotice(findChangeNotice(history))
                .build();
    }

    public FixedExpenseSavingsReportDto getSavingsReport(long userId, String rawYearMonth) {
        YearMonth yearMonth = resolveCurrentYearMonth(rawYearMonth);
        List<FixedExpenseReadRow> confirmedRows = mapper.findActiveItems(userId, null, true);
        if (confirmedRows == null) {
            confirmedRows = Collections.emptyList();
        }

        List<FixedExpenseSavingsItemDto> items = confirmedRows.stream()
                .map(this::toSavingsItem)
                .toList();
        BigDecimal monthlySavings = items.stream()
                .map(FixedExpenseSavingsItemDto::getSavingsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FixedExpenseSavingsReportDto.builder()
                .yearMonth(yearMonth.toString())
                .monthlySavings(monthlySavings)
                .yearlySavings(monthlySavings.multiply(MONTHS_PER_YEAR))
                .items(items)
                .build();
    }

    private YearMonth resolveCurrentYearMonth(String rawYearMonth) {
        YearMonth currentYearMonth = YearMonth.now(clock);
        if (rawYearMonth == null || rawYearMonth.isBlank()) {
            return currentYearMonth;
        }
        if (!YEAR_MONTH_PATTERN.matcher(rawYearMonth).matches()) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            YearMonth yearMonth = YearMonth.parse(rawYearMonth);
            if (!yearMonth.equals(currentYearMonth)) {
                throw new BusinessException("NOT_FOUND", "현재 월의 고정지출만 조회할 수 있습니다.",
                        HttpStatus.NOT_FOUND);
            }
            return yearMonth;
        } catch (BusinessException exception) {
            throw exception;
        } catch (DateTimeParseException exception) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private void validateCategoryId(Long categoryId) {
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException("INVALID_REQUEST", "categoryId는 양수여야 합니다.");
        }
    }

    private List<FixedExpenseItemDto> toItems(List<FixedExpenseReadRow> rows, boolean confirmed) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> toItem(row, confirmed))
                .toList();
    }

    private FixedExpenseItemDto toItem(FixedExpenseReadRow row, boolean confirmed) {
        String category = normalizeCategory(row.getCategoryName());
        LocalDate nextPaymentDate = row.getNextExpectedDate();
        return FixedExpenseItemDto.builder()
                .id(row.getId())
                .status(row.getStatus())
                .isExcluded(row.isExcluded())
                .isConfirmed(confirmed)
                .confirmedAt(row.getConfirmedAt())
                .categoryId(row.getCategoryId())
                .categoryCode(toCategoryCode(category))
                .categoryLabel(category.substring(0, 1))
                .name(row.getMerchantNameNormalized())
                .category(category)
                .description(confirmed ? "확정된 고정지출" : "최근 " + row.getDetectedCount() + "개월 반복")
                .averageAmount(row.getAvgAmount())
                .billingCycle(FixedExpenseBillingCycleDto.builder()
                        .type("monthly")
                        .day(nextPaymentDate == null ? 0 : nextPaymentDate.getDayOfMonth())
                        .build())
                .nextPaymentDate(nextPaymentDate)
                .paymentLabel(confirmed ? "월 결제" : "확인 필요")
                .caseNumber(confirmed ? null : "고정-" + row.getId())
                .evidenceCount(row.getDetectedCount())
                .build();
    }

    private FixedExpenseSavingsItemDto toSavingsItem(FixedExpenseReadRow row) {
        String category = normalizeCategory(row.getCategoryName());
        return FixedExpenseSavingsItemDto.builder()
                .id(row.getId())
                .categoryCode(toCategoryCode(category))
                .categoryLabel(category.substring(0, 1))
                .title(row.getMerchantNameNormalized())
                .description(category + " · 월 고정지출")
                .savingsAmount(row.getAvgAmount())
                .build();
    }

    private BigDecimal sumAverageAmount(List<FixedExpenseItemDto> items) {
        return items.stream()
                .map(FixedExpenseItemDto::getAverageAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FixedExpenseChangeNoticeDto findChangeNotice(List<FixedExpensePaymentHistoryDto> history) {
        if (history.size() < 2) {
            return null;
        }
        FixedExpensePaymentHistoryDto latest = history.get(0);
        FixedExpensePaymentHistoryDto previous = history.get(1);
        if (latest.getAmount() == null || previous.getAmount() == null || latest.getDate() == null) {
            return null;
        }
        BigDecimal difference = latest.getAmount().subtract(previous.getAmount()).abs();
        if (difference.signum() == 0) {
            return null;
        }
        return FixedExpenseChangeNoticeDto.builder()
                .month(latest.getDate().getMonthValue())
                .difference(difference)
                .build();
    }

    private List<FixedExpenseEvidenceMonthDto> toEvidenceMonths(
            List<FixedExpensePaymentHistoryDto> history, YearMonth currentYearMonth) {
        return IntStream.rangeClosed(0, 3)
                .mapToObj(offset -> currentYearMonth.minusMonths(3L - offset))
                .map(yearMonth -> FixedExpenseEvidenceMonthDto.builder()
                        .month(yearMonth.getMonthValue())
                        .detected(history.stream().anyMatch(payment -> payment.getDate() != null
                                && YearMonth.from(payment.getDate()).equals(yearMonth)))
                        .build())
                .toList();
    }

    private String normalizeCategory(String categoryName) {
        return categoryName == null || categoryName.isBlank() ? "미분류" : categoryName;
    }

    private String toCategoryCode(String categoryName) {
        if (categoryName.contains("통신")) {
            return "telecom";
        }
        if (categoryName.contains("구독") || categoryName.contains("문화") || categoryName.contains("여가")) {
            return "subscription";
        }
        if (categoryName.contains("생활") || categoryName.contains("쇼핑") || categoryName.contains("주거")) {
            return "living";
        }
        return "other";
    }
}

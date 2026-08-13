package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyCategorySpendingRow;
import com.kb.tangtang.report.domain.MonthlySpendingRow;
import com.kb.tangtang.report.dto.MonthlyCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlyParentCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendItemDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MonthlyReportService {

    private static final int TREND_MONTH_COUNT = 6;
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String STATUS_ONBOARDING = "ONBOARDING";
    private static final String STATUS_FIRST_REPORT = "FIRST_REPORT";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_CURRENT = "CURRENT";

    private final MonthlyReportMapper monthlyReportMapper;
    private final Clock clock;

    @Autowired
    public MonthlyReportService(MonthlyReportMapper monthlyReportMapper) {
        this(monthlyReportMapper, Clock.systemDefaultZone());
    }

    MonthlyReportService(MonthlyReportMapper monthlyReportMapper, Clock clock) {
        this.monthlyReportMapper = monthlyReportMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlySpendingTrendDto getSpendingTrend(long userId, String rawYearMonth) {
        ReportPeriod period = validateReportPeriod(userId, rawYearMonth);
        YearMonth startMonth = period.yearMonth.minusMonths(TREND_MONTH_COUNT - 1L);
        List<MonthlySpendingRow> rows = monthlyReportMapper.findMonthlySpending(
                userId, startMonth.atDay(1), period.yearMonth.plusMonths(1).atDay(1));
        Map<String, BigDecimal> amountByMonth = new HashMap<>();
        for (MonthlySpendingRow row : rows) {
            amountByMonth.put(row.getYearMonth(), nonNegative(row.getAmount()));
        }

        List<MonthlySpendingTrendItemDto> items = new ArrayList<>();
        for (int index = 0; index < TREND_MONTH_COUNT; index++) {
            YearMonth itemMonth = startMonth.plusMonths(index);
            boolean hasData = !itemMonth.isBefore(period.joinedMonth);
            items.add(MonthlySpendingTrendItemDto.builder()
                    .yearMonth(itemMonth.toString())
                    .amount(hasData ? amountByMonth.getOrDefault(itemMonth.toString(), BigDecimal.ZERO) : null)
                    .hasData(hasData)
                    .build());
        }

        return MonthlySpendingTrendDto.builder()
                .yearMonth(period.yearMonth.toString())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlySummaryDto getSummary(long userId, String rawYearMonth) {
        ReportPeriod period = validateReportPeriod(userId, rawYearMonth);
        BigDecimal totalSpent = sumMonth(userId, period.yearMonth);
        YearMonth previousMonth = period.yearMonth.minusMonths(1);
        boolean hasPreviousComparison = !previousMonth.isBefore(period.joinedMonth);
        BigDecimal previousMonthSpent = hasPreviousComparison ? sumMonth(userId, previousMonth) : null;

        return MonthlySummaryDto.builder()
                .yearMonth(period.yearMonth.toString())
                .totalSpent(totalSpent)
                .previousMonthSpent(previousMonthSpent)
                .hasPreviousComparison(hasPreviousComparison)
                .monthOverMonthRate(hasPreviousComparison
                        ? calculateChangeRate(totalSpent, previousMonthSpent)
                        : null)
                .fixedExpenseCandidateCount(
                        monthlyReportMapper.countActiveFixedExpenseCandidates(userId))
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyCategoryReportDto getCategories(long userId, String rawYearMonth) {
        ReportPeriod period = validateReportPeriod(userId, rawYearMonth);
        YearMonth previousMonth = period.yearMonth.minusMonths(1);
        List<MonthlyCategorySpendingRow> rows = monthlyReportMapper.findMonthlyCategorySpending(
                userId, previousMonth.atDay(1), period.yearMonth.plusMonths(1).atDay(1));

        Map<CategoryKey, BigDecimal> previousAmounts = new HashMap<>();
        Map<CategoryKey, MonthlyCategorySpendingRow> currentRows = new LinkedHashMap<>();
        Map<ParentCategoryKey, BigDecimal> currentParentAmounts = new HashMap<>();
        for (MonthlyCategorySpendingRow row : rows) {
            CategoryKey key = new CategoryKey(row.getCategoryId(), row.getCategoryName());
            if (previousMonth.toString().equals(row.getYearMonth())) {
                previousAmounts.put(key, nonNegative(row.getAmount()));
            } else if (period.yearMonth.toString().equals(row.getYearMonth())) {
                ParentCategoryKey parentKey = new ParentCategoryKey(
                        row.getParentCategoryId(), row.getParentCategoryName());
                currentParentAmounts.merge(parentKey, zeroIfNull(row.getAmount()), BigDecimal::add);
                if (nonNegative(row.getAmount()).signum() > 0) {
                    currentRows.put(key, row);
                }
            }
        }

        BigDecimal totalSpent = sumMonth(userId, period.yearMonth);
        boolean hasPreviousComparison = !previousMonth.isBefore(period.joinedMonth);
        List<MonthlyParentCategoryItemDto> parentCategories = new ArrayList<>();
        for (Map.Entry<ParentCategoryKey, BigDecimal> entry : currentParentAmounts.entrySet()) {
            BigDecimal amount = nonNegative(entry.getValue());
            if (amount.signum() <= 0) {
                continue;
            }
            parentCategories.add(MonthlyParentCategoryItemDto.builder()
                    .categoryId(entry.getKey().categoryId)
                    .categoryName(entry.getKey().categoryName)
                    .amount(amount)
                    .ratio(calculateRatio(amount, totalSpent))
                    .build());
        }
        parentCategories.sort((first, second) -> {
            int amountOrder = second.getAmount().compareTo(first.getAmount());
            return amountOrder != 0
                    ? amountOrder
                    : first.getCategoryName().compareTo(second.getCategoryName());
        });

        List<MonthlyCategoryItemDto> categories = new ArrayList<>();
        for (Map.Entry<CategoryKey, MonthlyCategorySpendingRow> entry : currentRows.entrySet()) {
            MonthlyCategorySpendingRow row = entry.getValue();
            BigDecimal amount = nonNegative(row.getAmount());
            BigDecimal previousAmount = hasPreviousComparison
                    ? previousAmounts.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                    : null;
            categories.add(MonthlyCategoryItemDto.builder()
                    .parentCategoryId(row.getParentCategoryId())
                    .parentCategoryName(row.getParentCategoryName())
                    .categoryId(row.getCategoryId())
                    .categoryName(row.getCategoryName())
                    .amount(amount)
                    .ratio(calculateRatio(amount, totalSpent))
                    .previousMonthAmount(previousAmount)
                    .changeRate(hasPreviousComparison
                            ? calculateChangeRate(amount, previousAmount)
                            : null)
                    .build());
        }

        categories.sort((first, second) -> {
            int amountOrder = second.getAmount().compareTo(first.getAmount());
            return amountOrder != 0
                    ? amountOrder
                    : first.getCategoryName().compareTo(second.getCategoryName());
        });

        return MonthlyCategoryReportDto.builder()
                .yearMonth(period.yearMonth.toString())
                .totalSpent(totalSpent)
                .parentCategories(parentCategories)
                .categories(categories)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyReportMonthsDto getAvailableMonths(long userId) {
        YearMonth joinedMonth = findJoinedMonth(userId);
        YearMonth currentMonth = YearMonth.now(clock);
        List<MonthlyReportMonthDto> months = new ArrayList<>();
        for (YearMonth month = currentMonth; !month.isBefore(joinedMonth); month = month.minusMonths(1)) {
            boolean completed = month.isBefore(currentMonth);
            String status;
            if (!completed) {
                status = month.equals(joinedMonth) ? STATUS_ONBOARDING : STATUS_CURRENT;
            } else {
                status = month.equals(joinedMonth) ? STATUS_FIRST_REPORT : STATUS_READY;
            }
            months.add(MonthlyReportMonthDto.builder()
                    .value(month.toString())
                    .year(month.getYear())
                    .month(month.getMonthValue())
                    .available(!STATUS_CURRENT.equals(status))
                    .hasReport(completed)
                    .status(status)
                    .build());
        }
        return MonthlyReportMonthsDto.builder().months(months).build();
    }

    private ReportPeriod validateReportPeriod(long userId, String rawYearMonth) {
        YearMonth yearMonth = parseYearMonth(rawYearMonth);
        YearMonth joinedMonth = findJoinedMonth(userId);
        if (yearMonth.isBefore(joinedMonth)) {
            throw new BusinessException("REPORT_NOT_AVAILABLE", "가입 이전 월의 리포트는 조회할 수 없습니다.");
        }
        if (!yearMonth.isBefore(YearMonth.now(clock))) {
            throw new BusinessException("REPORT_NOT_AVAILABLE", "완료된 월의 리포트만 조회할 수 있습니다.");
        }
        return new ReportPeriod(yearMonth, joinedMonth);
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        if (rawYearMonth == null || !YEAR_MONTH_PATTERN.matcher(rawYearMonth).matches()) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private YearMonth findJoinedMonth(long userId) {
        LocalDate createdDate = monthlyReportMapper.findUserCreatedDate(userId);
        if (createdDate == null) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return YearMonth.from(createdDate);
    }

    private BigDecimal sumMonth(long userId, YearMonth month) {
        return nonNegative(monthlyReportMapper.sumNetSpending(
                userId, month.atDay(1), month.plusMonths(1).atDay(1)));
    }

    private BigDecimal calculateRatio(BigDecimal amount, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current.signum() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : null;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static class ReportPeriod {
        private final YearMonth yearMonth;
        private final YearMonth joinedMonth;

        private ReportPeriod(YearMonth yearMonth, YearMonth joinedMonth) {
            this.yearMonth = yearMonth;
            this.joinedMonth = joinedMonth;
        }
    }

    private static class CategoryKey {
        private final Long categoryId;
        private final String categoryName;

        private CategoryKey(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CategoryKey)) {
                return false;
            }
            CategoryKey that = (CategoryKey) other;
            return java.util.Objects.equals(categoryId, that.categoryId)
                    && java.util.Objects.equals(categoryName, that.categoryName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(categoryId, categoryName);
        }
    }

    private static class ParentCategoryKey {
        private final Long categoryId;
        private final String categoryName;

        private ParentCategoryKey(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ParentCategoryKey)) {
                return false;
            }
            ParentCategoryKey that = (ParentCategoryKey) other;
            return java.util.Objects.equals(categoryId, that.categoryId)
                    && java.util.Objects.equals(categoryName, that.categoryName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(categoryId, categoryName);
        }
    }
}

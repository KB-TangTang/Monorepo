package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisCategory;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisInput;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.domain.MonthlyCategorySpendingRow;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** 수동 생성, 저장 결과 재사용, 실패 재시도와 중복 생성을 담당한다. */
@Service
public class MonthlyAiAnalysisService {

    private static final String STATUS_NOT_REQUESTED = "NOT_REQUESTED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_NOT_CONSENTED = "NOT_CONSENTED";
    private static final String PROVIDER_OPENAI = "OPENAI";
    private static final String PROMPT_VERSION = "monthly-report-ai-v10";
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final MonthlyReportMapper monthlyReportMapper;
    private final MonthlyAiAnalysisProvider provider;
    private final MonthlyAiAnalysisStateService stateService;
    private final MonthlyAiAnalysisSnapshotService snapshotService;
    private final MonthlyAiAnalysisResultReader resultReader;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String model;

    @Autowired
    public MonthlyAiAnalysisService(MonthlyReportMapper monthlyReportMapper,
                                    MonthlyAiAnalysisProvider provider,
                                    MonthlyAiAnalysisStateService stateService,
                                    MonthlyAiAnalysisSnapshotService snapshotService,
                                    MonthlyAiAnalysisResultReader resultReader,
                                    ObjectMapper objectMapper,
                                    @Value("${openai.model:gpt-5-nano}") String model,
                                    @Value("${report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(monthlyReportMapper, provider, stateService, snapshotService, resultReader,
                objectMapper, Clock.system(ZoneId.of(zoneId)), model);
    }

    MonthlyAiAnalysisService(MonthlyReportMapper monthlyReportMapper,
                             MonthlyAiAnalysisProvider provider,
                             MonthlyAiAnalysisStateService stateService,
                             MonthlyAiAnalysisSnapshotService snapshotService,
                             MonthlyAiAnalysisResultReader resultReader,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this(monthlyReportMapper, provider, stateService, snapshotService, resultReader,
                objectMapper, clock, "gpt-5-nano");
    }

    MonthlyAiAnalysisService(MonthlyReportMapper monthlyReportMapper,
                             MonthlyAiAnalysisProvider provider,
                             MonthlyAiAnalysisStateService stateService,
                             MonthlyAiAnalysisSnapshotService snapshotService,
                             MonthlyAiAnalysisResultReader resultReader,
                             ObjectMapper objectMapper,
                             Clock clock,
                             String model) {
        this.monthlyReportMapper = monthlyReportMapper;
        this.provider = provider;
        this.stateService = stateService;
        this.snapshotService = snapshotService;
        this.resultReader = resultReader;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.model = model;
    }

    public MonthlyAiAnalysisDto generate(long userId, String rawYearMonth) {
        ReportPeriod period = validateReportPeriod(userId, rawYearMonth);
        return generate(userId, period);
    }

    private MonthlyAiAnalysisDto generate(long userId, ReportPeriod period) {
        MonthlyAiAnalysisSnapshot initialSnapshot = monthlyReportMapper.findAiAnalysisSnapshot(
                userId, period.yearMonth.toString());
        if (initialSnapshot == null || STATUS_NOT_CONSENTED.equals(initialSnapshot.getAiAnalysisStatus())) {
            return notConsentedResult(period.yearMonth.toString());
        }
        if (initialSnapshot != null && STATUS_COMPLETED.equals(initialSnapshot.getAiAnalysisStatus())) {
            return resultReader.readCompleted(initialSnapshot, period.yearMonth.toString());
        }
        if (initialSnapshot != null && STATUS_IN_PROGRESS.equals(initialSnapshot.getAiAnalysisStatus())) {
            throwInProgress();
        }
        if (STATUS_NOT_REQUESTED.equals(initialSnapshot.getAiAnalysisStatus())
                || STATUS_FAILED.equals(initialSnapshot.getAiAnalysisStatus())) {
            snapshotService.saveSnapshot(userId, period.yearMonth.toString(), true);
        }

        return generateUsingPreparedSnapshot(userId, period);
    }

    /** 배치가 저장한 스냅샷을 사용해 AI 상태 전이와 생성을 수행한다. */
    public MonthlyAiAnalysisDto generateUsingPreparedSnapshot(long userId, String rawYearMonth) {
        return generateUsingPreparedSnapshot(userId, validateReportPeriod(userId, rawYearMonth));
    }

    private MonthlyAiAnalysisDto generateUsingPreparedSnapshot(long userId, ReportPeriod period) {
        MonthlyAiAnalysisSnapshot snapshot = requireSnapshot(userId, period.yearMonth);
        if (STATUS_NOT_CONSENTED.equals(snapshot.getAiAnalysisStatus())) {
            return notConsentedResult(period.yearMonth.toString());
        }
        if (STATUS_COMPLETED.equals(snapshot.getAiAnalysisStatus())) {
            return resultReader.readCompleted(snapshot, period.yearMonth.toString());
        }
        if (STATUS_IN_PROGRESS.equals(snapshot.getAiAnalysisStatus())) {
            throwInProgress();
        }

        MonthlyAiAnalysisInput input = buildInput(userId, period);
        String inputHash = calculateInputHash(input);
        int claimed = stateService.claim(
                userId, period.yearMonth.toString(), PROVIDER_OPENAI, model, PROMPT_VERSION, inputHash);
        if (claimed == 0) {
            return resolveUnclaimedGeneration(userId, period.yearMonth);
        }

        try {
            MonthlyAiAnalysisDto result = provider.generate(input);
            String feedbacksJson = objectMapper.writeValueAsString(result.getFeedbacks());
            int saved = stateService.complete(
                    userId, period.yearMonth.toString(), feedbacksJson, result.getSavingsAnalogy());
            if (saved != 1) {
                throw new AiProviderException("AI_PROVIDER_UNAVAILABLE",
                        "AI 분석 결과를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            return MonthlyAiAnalysisDto.builder()
                    .yearMonth(period.yearMonth.toString())
                    .status(STATUS_COMPLETED)
                    .feedbacks(result.getFeedbacks())
                    .savingsAnalogy(result.getSavingsAnalogy())
                    .build();
        } catch (AiProviderException ex) {
            stateService.fail(userId, period.yearMonth.toString(), ex.getCode());
            throw new BusinessException(ex.getCode(), ex.getMessage(), ex.getHttpStatus());
        } catch (Exception ex) {
            stateService.fail(userId, period.yearMonth.toString(), "AI_PROVIDER_UNAVAILABLE");
            throw new BusinessException("AI_PROVIDER_UNAVAILABLE",
                    "AI 분석 결과를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private MonthlyAiAnalysisDto resolveUnclaimedGeneration(long userId, YearMonth yearMonth) {
        MonthlyAiAnalysisSnapshot currentSnapshot = requireSnapshot(userId, yearMonth);
        if (STATUS_COMPLETED.equals(currentSnapshot.getAiAnalysisStatus())) {
            return resultReader.readCompleted(currentSnapshot, yearMonth.toString());
        }
        if (STATUS_IN_PROGRESS.equals(currentSnapshot.getAiAnalysisStatus())) {
            throw new BusinessException("AI_ANALYSIS_IN_PROGRESS",
                    "해당 월의 AI 분석을 생성 중입니다.", HttpStatus.CONFLICT);
        }
        throw new BusinessException("AI_ANALYSIS_IN_PROGRESS",
                "해당 월의 AI 분석 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요.", HttpStatus.CONFLICT);
    }

    private MonthlyAiAnalysisSnapshot requireSnapshot(long userId, YearMonth yearMonth) {
        MonthlyAiAnalysisSnapshot snapshot = monthlyReportMapper.findAiAnalysisSnapshot(userId, yearMonth.toString());
        if (snapshot == null) {
            throw new BusinessException("NOT_FOUND", "월간 리포트 스냅샷을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        return snapshot;
    }

    private void throwInProgress() {
        throw new BusinessException("AI_ANALYSIS_IN_PROGRESS",
                "해당 월의 AI 분석이 생성 중입니다.", HttpStatus.CONFLICT);
    }

    private MonthlyAiAnalysisDto notConsentedResult(String yearMonth) {
        return MonthlyAiAnalysisDto.builder()
                .yearMonth(yearMonth)
                .status(STATUS_NOT_CONSENTED)
                .feedbacks(List.of())
                .savingsAnalogy(null)
                .build();
    }

    private MonthlyAiAnalysisInput buildInput(long userId, ReportPeriod period) {
        BigDecimal currentMonthSpent = sumMonth(userId, period.yearMonth);
        YearMonth previousMonth = period.yearMonth.minusMonths(1);
        boolean hasPreviousComparison = !previousMonth.isBefore(period.joinedMonth);
        BigDecimal previousMonthSpent = hasPreviousComparison ? sumMonth(userId, previousMonth) : null;
        BigDecimal savingsAmount = hasPreviousComparison && previousMonthSpent.compareTo(currentMonthSpent) > 0
                ? previousMonthSpent.subtract(currentMonthSpent)
                : BigDecimal.ZERO;

        List<MonthlyAiAnalysisCategory> categories = new ArrayList<>();
        List<MonthlyCategorySpendingRow> rows = monthlyReportMapper.findMonthlyCategorySpending(
                userId, period.yearMonth.atDay(1), period.yearMonth.plusMonths(1).atDay(1));
        for (MonthlyCategorySpendingRow row : rows) {
            if (period.yearMonth.toString().equals(row.getYearMonth()) && nonNegative(row.getAmount()).signum() > 0) {
                categories.add(MonthlyAiAnalysisCategory.builder()
                        .parentCategoryName(row.getParentCategoryName())
                        .categoryName(row.getCategoryName())
                        .amount(nonNegative(row.getAmount()))
                        .build());
            }
        }
        categories.sort(Comparator.comparing(MonthlyAiAnalysisCategory::getAmount).reversed()
                .thenComparing(MonthlyAiAnalysisCategory::getCategoryName,
                        Comparator.nullsLast(String::compareTo)));

        return MonthlyAiAnalysisInput.builder()
                .yearMonth(period.yearMonth.toString())
                .currentMonthSpent(currentMonthSpent)
                .previousMonthSpent(previousMonthSpent)
                .hasPreviousComparison(hasPreviousComparison)
                .savingsAmount(savingsAmount)
                .categories(categories)
                .build();
    }

    private ReportPeriod validateReportPeriod(long userId, String rawYearMonth) {
        if (rawYearMonth == null || !YEAR_MONTH_PATTERN.matcher(rawYearMonth).matches()) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            YearMonth yearMonth = YearMonth.parse(rawYearMonth);
            LocalDate createdDate = monthlyReportMapper.findUserCreatedDate(userId);
            if (createdDate == null) {
                throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
            }
            YearMonth joinedMonth = YearMonth.from(createdDate);
            if (yearMonth.isBefore(joinedMonth) || !yearMonth.isBefore(YearMonth.now(clock))) {
                throw new BusinessException("NOT_FOUND", "조회할 수 없는 월간 리포트입니다.", HttpStatus.NOT_FOUND);
            }
            return new ReportPeriod(yearMonth, joinedMonth);
        } catch (BusinessException ex) {
            throw ex;
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private BigDecimal sumMonth(long userId, YearMonth month) {
        return nonNegative(monthlyReportMapper.sumNetSpending(
                userId, month.atDay(1), month.plusMonths(1).atDay(1)));
    }

    private String calculateInputHash(MonthlyAiAnalysisInput input) {
        try {
            byte[] source = objectMapper.writeValueAsBytes(input);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (Exception ex) {
            throw new BusinessException("AI_PROVIDER_UNAVAILABLE",
                    "AI 분석 요청을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null || amount.signum() <= 0 ? BigDecimal.ZERO : amount;
    }

    private static class ReportPeriod {
        private final YearMonth yearMonth;
        private final YearMonth joinedMonth;

        private ReportPeriod(YearMonth yearMonth, YearMonth joinedMonth) {
            this.yearMonth = yearMonth;
            this.joinedMonth = joinedMonth;
        }
    }
}

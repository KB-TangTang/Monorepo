package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/** 저장된 AI 분석 결과만 조회하며 스냅샷이나 외부 AI를 변경하지 않는다. */
@Service
public class MonthlyAiAnalysisQueryService {

    private static final String STATUS_NOT_REQUESTED = "NOT_REQUESTED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final MonthlyReportMapper monthlyReportMapper;
    private final MonthlyAiAnalysisResultReader resultReader;
    private final Clock clock;

    @Autowired
    public MonthlyAiAnalysisQueryService(MonthlyReportMapper monthlyReportMapper,
                                         MonthlyAiAnalysisResultReader resultReader) {
        this(monthlyReportMapper, resultReader, Clock.systemDefaultZone());
    }

    MonthlyAiAnalysisQueryService(MonthlyReportMapper monthlyReportMapper,
                                  MonthlyAiAnalysisResultReader resultReader,
                                  Clock clock) {
        this.monthlyReportMapper = monthlyReportMapper;
        this.resultReader = resultReader;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlyAiAnalysisDto get(long userId, String rawYearMonth) {
        validateReportPeriod(userId, rawYearMonth);
        MonthlyAiAnalysisSnapshot snapshot = monthlyReportMapper.findAiAnalysisSnapshot(userId, rawYearMonth);
        if (snapshot == null) {
            return emptyResult(rawYearMonth, STATUS_NOT_REQUESTED);
        }
        if (STATUS_COMPLETED.equals(snapshot.getAiAnalysisStatus())) {
            return resultReader.readCompleted(snapshot, rawYearMonth);
        }
        return emptyResult(rawYearMonth, snapshot.getAiAnalysisStatus());
    }

    private MonthlyAiAnalysisDto emptyResult(String yearMonth, String status) {
        return MonthlyAiAnalysisDto.builder()
                .yearMonth(yearMonth)
                .status(status == null ? STATUS_NOT_REQUESTED : status)
                .feedbacks(List.of())
                .savingsAnalogy(null)
                .build();
    }

    private YearMonth validateReportPeriod(long userId, String rawYearMonth) {
        if (rawYearMonth == null || !YEAR_MONTH_PATTERN.matcher(rawYearMonth).matches()) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            YearMonth yearMonth = YearMonth.parse(rawYearMonth);
            LocalDate createdDate = monthlyReportMapper.findUserCreatedDate(userId);
            if (createdDate == null) {
                throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
            }
            if (yearMonth.isBefore(YearMonth.from(createdDate)) || !yearMonth.isBefore(YearMonth.now(clock))) {
                throw new BusinessException("NOT_FOUND", "조회할 수 없는 월간 리포트입니다.");
            }
            return yearMonth;
        } catch (BusinessException ex) {
            throw ex;
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}

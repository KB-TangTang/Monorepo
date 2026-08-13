package com.kb.tangtang.report.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/**
 * {@code MonthlyReportController} 의 Swagger 문서.
 *
 * <p>모든 엔드포인트가 {@code yearMonth} 를 {@code YYYY-MM} 형식으로 받는다.
 */
@Api(tags = "10. 월간 리포트(판결문) — 소비 추이 · 요약 · AI 분석")
public interface MonthlyReportControllerDocs {

    @ApiOperation(value = "월별 소비 추이", notes = "해당 월을 포함한 최근 6개월 추이를 내려준다.")
    ApiResponse<MonthlySpendingTrendDto> getSpendingTrend(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회 연월 (YYYY-MM)", required = true, example = "2026-08") String yearMonth);

    @ApiOperation(value = "월간 소비 요약", notes = "총 소비액·전월 대비 증감 등 리포트 상단 지표다.")
    ApiResponse<MonthlySummaryDto> getSummary(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회 연월 (YYYY-MM)", required = true, example = "2026-08") String yearMonth);

    @ApiOperation(value = "월간 카테고리별 소비", notes = "카테고리 소분류 단위 집계다.")
    ApiResponse<MonthlyCategoryReportDto> getCategories(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회 연월 (YYYY-MM)", required = true, example = "2026-08") String yearMonth);

    @ApiOperation(value = "조회 가능한 월 목록",
            notes = "거래내역이 있는 월만 내려온다. 리포트 화면의 월 선택기가 쓴다.")
    ApiResponse<MonthlyReportMonthsDto> getAvailableMonths(@ApiIgnore Long userId);

    @ApiOperation(value = "AI 소비 분석 생성",
            notes = "**외부 LLM 을 호출하므로 응답이 느리고 비용이 든다.** 화면 진입 때마다 부르지 말 것.\n\n"
                    + "생성 결과는 DB 에 저장된다. 이미 생성돼 있으면 저장본을 그대로 돌려준다.\n"
                    + "저장된 결과만 필요하면 아래 조회 API 를 쓴다.")
    ApiResponse<MonthlyAiAnalysisDto> generateAiAnalysis(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회 연월 (YYYY-MM)", required = true, example = "2026-08") String yearMonth);

    @ApiOperation(value = "AI 소비 분석 조회",
            notes = "**저장된 결과만 읽는다. 없으면 생성하지 않는다** — LLM 을 호출하지 않으므로 빠르다.")
    ApiResponse<MonthlyAiAnalysisDto> getAiAnalysis(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회 연월 (YYYY-MM)", required = true, example = "2026-08") String yearMonth);
}

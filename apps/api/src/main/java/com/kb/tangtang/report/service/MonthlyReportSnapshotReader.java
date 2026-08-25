package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotContent;
import com.kb.tangtang.report.dto.MonthlyCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyParentCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendItemDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** category_summary_json의 버전 2 월간 리포트 페이로드를 안전하게 읽는다. */
@Component
public class MonthlyReportSnapshotReader {

    public static final int SNAPSHOT_VERSION = 2;

    private final ObjectMapper objectMapper;

    public MonthlyReportSnapshotReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MonthlyReportSnapshotContent read(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || root.path("snapshotVersion").asInt(-1) != SNAPSHOT_VERSION
                    || !root.path("aiUsageConsented").isBoolean()) {
                return null;
            }

            JsonNode summary = root.path("summary");
            JsonNode spendingTrend = root.path("spendingTrend");
            JsonNode categoryReport = root.path("categoryReport");
            if (!summary.isObject() || !spendingTrend.isObject() || !categoryReport.isObject()) {
                return null;
            }

            return new MonthlyReportSnapshotContent(
                    root.path("aiUsageConsented").asBoolean(),
                    toSummary(summary),
                    toSpendingTrend(spendingTrend),
                    toCategoryReport(categoryReport));
        } catch (Exception exception) {
            return null;
        }
    }

    private MonthlySummaryDto toSummary(JsonNode node) {
        return MonthlySummaryDto.builder()
                .yearMonth(text(node, "yearMonth"))
                .totalSpent(decimal(node, "totalSpent"))
                .previousMonthSpent(nullableDecimal(node, "previousMonthSpent"))
                .hasPreviousComparison(node.path("hasPreviousComparison").asBoolean())
                .monthOverMonthRate(nullableDecimal(node, "monthOverMonthRate"))
                .fixedExpenseCandidateCount(node.path("fixedExpenseCandidateCount").asInt())
                .confirmedFixedExpenseCount(node.path("confirmedFixedExpenseCount").asInt())
                .build();
    }

    private MonthlySpendingTrendDto toSpendingTrend(JsonNode node) {
        List<MonthlySpendingTrendItemDto> items = new ArrayList<>();
        for (JsonNode item : node.path("items")) {
            items.add(MonthlySpendingTrendItemDto.builder()
                    .yearMonth(text(item, "yearMonth"))
                    .amount(nullableDecimal(item, "amount"))
                    .hasData(item.path("hasData").asBoolean())
                    .build());
        }
        return MonthlySpendingTrendDto.builder()
                .yearMonth(text(node, "yearMonth"))
                .items(items)
                .build();
    }

    private MonthlyCategoryReportDto toCategoryReport(JsonNode node) {
        List<MonthlyParentCategoryItemDto> parentCategories = new ArrayList<>();
        for (JsonNode item : node.path("parentCategories")) {
            parentCategories.add(MonthlyParentCategoryItemDto.builder()
                    .categoryId(nullableLong(item, "categoryId"))
                    .categoryName(text(item, "categoryName"))
                    .amount(decimal(item, "amount"))
                    .ratio(decimal(item, "ratio"))
                    .build());
        }

        List<MonthlyCategoryItemDto> categories = new ArrayList<>();
        for (JsonNode item : node.path("categories")) {
            categories.add(MonthlyCategoryItemDto.builder()
                    .parentCategoryId(nullableLong(item, "parentCategoryId"))
                    .parentCategoryName(text(item, "parentCategoryName"))
                    .categoryId(nullableLong(item, "categoryId"))
                    .categoryName(text(item, "categoryName"))
                    .amount(decimal(item, "amount"))
                    .ratio(decimal(item, "ratio"))
                    .previousMonthAmount(nullableDecimal(item, "previousMonthAmount"))
                    .changeRate(nullableDecimal(item, "changeRate"))
                    .build());
        }

        return MonthlyCategoryReportDto.builder()
                .yearMonth(text(node, "yearMonth"))
                .totalSpent(decimal(node, "totalSpent"))
                .parentCategories(parentCategories)
                .categories(categories)
                .build();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        BigDecimal value = nullableDecimal(node, fieldName);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nullableDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.decimalValue() : null;
    }

    private Long nullableLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.longValue() : null;
    }
}

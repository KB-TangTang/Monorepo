package com.kb.tangtang.report.controller;

import com.kb.tangtang.report.dto.MonthlyReportMonthDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import com.kb.tangtang.report.service.MonthlyReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyReportControllerTest {

    private static final long USER_ID = 7L;

    private HandlerMethodArgumentResolver loginUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return USER_ID;
            }
        };
    }

    private static class StubService extends MonthlyReportService {
        StubService() {
            super((MonthlyReportMapper) null);
        }

        @Override
        public MonthlySummaryDto getSummary(long userId, String yearMonth) {
            return MonthlySummaryDto.builder()
                    .yearMonth(yearMonth)
                    .totalSpent(new BigDecimal("1284000"))
                    .previousMonthSpent(new BigDecimal("1396000"))
                    .hasPreviousComparison(true)
                    .monthOverMonthRate(new BigDecimal("-8.02"))
                    .fixedExpenseCandidateCount(2)
                    .build();
        }

        @Override
        public MonthlyReportMonthsDto getAvailableMonths(long userId) {
            return MonthlyReportMonthsDto.builder()
                    .months(List.of(MonthlyReportMonthDto.builder()
                            .value("2026-07")
                            .year(2026)
                            .month(7)
                            .available(true)
                            .hasReport(true)
                            .status("report")
                            .build()))
                    .build();
        }
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new MonthlyReportController(new StubService()))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();
    }

    @Test
    @DisplayName("월간 요약은 인증 사용자와 yearMonth를 받아 ApiResponse로 반환한다")
    void returnsMonthlySummary() throws Exception {
        mockMvc().perform(get("/api/reports/monthly/summary")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-07"))
                .andExpect(jsonPath("$.data.totalSpent").value(1284000))
                .andExpect(jsonPath("$.data.monthOverMonthRate").value(-8.02))
                .andExpect(jsonPath("$.data.fixedExpenseCandidateCount").value(2));
    }

    @Test
    @DisplayName("조회 가능한 월 목록을 ApiResponse로 반환한다")
    void returnsAvailableMonths() throws Exception {
        mockMvc().perform(get("/api/reports/monthly/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.months[0].value").value("2026-07"))
                .andExpect(jsonPath("$.data.months[0].available").value(true));
    }
}

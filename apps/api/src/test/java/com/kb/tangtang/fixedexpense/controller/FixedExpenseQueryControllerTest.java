package com.kb.tangtang.fixedexpense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewSummaryDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsReportDto;
import com.kb.tangtang.fixedexpense.service.FixedExpenseQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FixedExpenseQueryControllerTest {

    private static final long USER_ID = 7L;
    private final FixedExpenseQueryService service = mock(FixedExpenseQueryService.class);

    private MockMvc mockMvc() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders.standaloneSetup(new FixedExpenseQueryController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

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

    @Test
    void returnsOverviewWithLoggedInUserAndQueryFilters() throws Exception {
        FixedExpenseOverviewDto result = FixedExpenseOverviewDto.builder()
                .yearMonth("2026-08")
                .summary(FixedExpenseOverviewSummaryDto.builder()
                        .expectedMonthlyAmount(new BigDecimal("31900"))
                        .confirmedCount(1)
                        .candidateCount(1)
                        .build())
                .confirmed(List.of())
                .candidates(List.of())
                .build();
        when(service.getOverview(USER_ID, "2026-08", 3L)).thenReturn(result);

        mockMvc().perform(get("/api/fixedExpenses/candidates")
                        .queryParam("yearMonth", "2026-08")
                        .queryParam("categoryId", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-08"))
                .andExpect(jsonPath("$.data.summary.expectedMonthlyAmount").value(31900));

        verify(service).getOverview(USER_ID, "2026-08", 3L);
    }

    @Test
    void returnsSavingsReportWithLoggedInUser() throws Exception {
        when(service.getSavingsReport(USER_ID, null)).thenReturn(FixedExpenseSavingsReportDto.builder()
                .yearMonth("2026-08")
                .monthlySavings(BigDecimal.ZERO)
                .yearlySavings(BigDecimal.ZERO)
                .items(List.of())
                .build());

        mockMvc().perform(get("/api/fixedExpenses/savingReport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlySavings").value(0))
                .andExpect(jsonPath("$.data.items").isArray());

        verify(service).getSavingsReport(USER_ID, null);
    }
}

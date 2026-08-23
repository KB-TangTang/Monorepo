package com.kb.tangtang.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.transaction.dto.DailySpendingSummaryDto;
import com.kb.tangtang.transaction.dto.LedgerSummaryDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.dto.TransactionListResponseDto;
import com.kb.tangtang.transaction.dto.TransactionMonthDto;
import com.kb.tangtang.transaction.dto.TransactionMonthsDto;
import com.kb.tangtang.transaction.service.TransactionQueryService;
import com.kb.tangtang.transaction.service.TransactionService;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {

    private static final long USER_ID = 7L;
    private final TransactionService service = mock(TransactionService.class);
    private final TransactionQueryService queryService = mock(TransactionQueryService.class);

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new TransactionController(service, queryService))
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
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
    void updatesCategoryWithLoggedInUserAndReturnsApiResponse() throws Exception {
        when(service.updateCategory(USER_ID, 501L, 12L, true))
                .thenReturn(TransactionCategoryUpdateResultDto.builder()
                        .transactionId(501L)
                        .categoryId(12L)
                        .categorySource("USER")
                        .merchantRuleApplied(true)
                        .build());

        mockMvc().perform(patch("/api/transactions/501/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":12,\"applyToMerchant\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value(501))
                .andExpect(jsonPath("$.data.categoryId").value(12))
                .andExpect(jsonPath("$.data.categorySource").value("USER"))
                .andExpect(jsonPath("$.data.merchantRuleApplied").value(true));

        verify(service).updateCategory(USER_ID, 501L, 12L, true);
    }

    @Test
    void getAvailableMonthsReturnsQueryServiceResult() throws Exception {
        when(queryService.getAvailableMonths(USER_ID)).thenReturn(TransactionMonthsDto.builder()
                .months(List.of(TransactionMonthDto.builder().value("2026-07").hasData(true).build()))
                .build());

        mockMvc().perform(get("/api/transactions/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.months[0].value").value("2026-07"))
                .andExpect(jsonPath("$.data.months[0].hasData").value(true));
    }

    @Test
    void getTransactionsPassesYearMonthQueryParamThrough() throws Exception {
        when(queryService.getTransactions(USER_ID, "2026-07")).thenReturn(TransactionListResponseDto.builder()
                .period("2026-07")
                .summary(LedgerSummaryDto.builder()
                        .totalSpent(BigDecimal.valueOf(10000))
                        .totalDeposit(BigDecimal.ZERO)
                        .monthOverMonthRate(BigDecimal.ZERO)
                        .paymentMethods(Collections.emptyList())
                        .build())
                .transactions(Collections.emptyList())
                .build());

        mockMvc().perform(get("/api/transactions").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("2026-07"))
                .andExpect(jsonPath("$.data.summary.totalSpent").value(10000));
    }

    @Test
    void getTransactionsWithoutYearMonthCallsQueryServiceWithNull() throws Exception {
        when(queryService.getTransactions(eq(USER_ID), isNull())).thenReturn(TransactionListResponseDto.builder()
                .period(null)
                .summary(null)
                .transactions(Collections.emptyList())
                .build());

        mockMvc().perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").doesNotExist());
    }

    @Test
    void getDailySpendingSummaryReturnsHomeCardFields() throws Exception {
        when(queryService.getDailySpendingSummary(USER_ID)).thenReturn(DailySpendingSummaryDto.builder()
                .date("2026-08-15")
                .todayAmount(BigDecimal.valueOf(32000))
                .monthAmount(BigDecimal.valueOf(486300))
                .changeRate(new BigDecimal("-20.00"))
                .build());

        mockMvc().perform(get("/api/transactions/summary/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2026-08-15"))
                .andExpect(jsonPath("$.data.todayAmount").value(32000))
                .andExpect(jsonPath("$.data.monthAmount").value(486300))
                .andExpect(jsonPath("$.data.changeRate").value(-20.00));

        verify(queryService).getDailySpendingSummary(USER_ID);
    }

    @Test
    void getDailySpendingSummaryKeepsNullChangeRateOnTheWire() throws Exception {
        /*
         * ApiResponse 의 NON_NULL 은 래퍼 필드(code/message)에만 걸린다 — data 안의 null 은 그대로
         * 실려야 한다. 키가 통째로 빠지면 프론트가 undefined 를 받아 「어제와 비슷하게」로 오독할 여지가 생긴다.
         */
        when(queryService.getDailySpendingSummary(USER_ID)).thenReturn(DailySpendingSummaryDto.builder()
                .date("2026-08-15")
                .todayAmount(BigDecimal.ZERO)
                .monthAmount(BigDecimal.ZERO)
                .changeRate(null)
                .build());

        String body = mockMvc().perform(get("/api/transactions/summary/daily"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("\"changeRate\":null"), "changeRate 는 null 로 실려 나가야 한다: " + body);
    }
}

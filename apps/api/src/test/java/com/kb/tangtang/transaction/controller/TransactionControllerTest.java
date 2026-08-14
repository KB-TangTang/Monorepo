package com.kb.tangtang.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {

    private static final long USER_ID = 7L;
    private final TransactionService service = mock(TransactionService.class);

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new TransactionController(service))
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
}

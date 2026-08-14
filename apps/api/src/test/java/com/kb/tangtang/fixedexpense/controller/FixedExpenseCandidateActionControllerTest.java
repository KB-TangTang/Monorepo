package com.kb.tangtang.fixedexpense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionResponseDto;
import com.kb.tangtang.fixedexpense.service.FixedExpenseCandidateActionService;
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

class FixedExpenseCandidateActionControllerTest {

    private static final long USER_ID = 7L;
    private final FixedExpenseCandidateActionService service = mock(FixedExpenseCandidateActionService.class);

    private MockMvc mockMvc() {
        ObjectMapper responseObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders.standaloneSetup(new FixedExpenseCandidateActionController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(responseObjectMapper))
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
    void confirmsCandidateWithLoggedInUserAndReturnsApiResponse() throws Exception {
        when(service.decide(USER_ID, 101L, "CONFIRM"))
                .thenReturn(FixedExpenseCandidateActionResponseDto.builder()
                        .candidateId(101L)
                        .status("ACTIVE")
                        .isExcluded(false)
                        .confirmedAt(java.time.LocalDateTime.of(2026, 8, 14, 10, 2, 3))
                        .build());

        mockMvc().perform(patch("/api/fixedExpenses/candidates/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CONFIRM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateId").value(101))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.isExcluded").value(false))
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-08-14T10:02:03"));

        verify(service).decide(USER_ID, 101L, "CONFIRM");
    }
}

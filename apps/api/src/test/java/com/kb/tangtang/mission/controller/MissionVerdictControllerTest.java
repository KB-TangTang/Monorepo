package com.kb.tangtang.mission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.mission.dto.MissionVerdictAcknowledgeDto;
import com.kb.tangtang.mission.dto.MissionVerdictDto;
import com.kb.tangtang.mission.service.MissionVerdictService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MissionVerdictControllerTest {

    private static final long USER_ID = 7L;

    @Test
    void returnsPendingVerdictWithApiResponse() throws Exception {
        MissionVerdictService service = mock(MissionVerdictService.class);
        when(service.getPendingVerdict(USER_ID)).thenReturn(MissionVerdictDto.builder()
                .assignmentId(123L)
                .result("SUCCESS")
                .assignDate(LocalDate.of(2026, 8, 13))
                .currentAmount(new BigDecimal("9800"))
                .targetValue(new BigDecimal("12000"))
                .transactions(List.of())
                .build());

        mockMvc(service).perform(get("/api/missions/verdicts/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentId").value(123))
                .andExpect(jsonPath("$.data.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.assignDate").value("2026-08-13"));
    }

    @Test
    void acknowledgesVerdictWithApiResponse() throws Exception {
        MissionVerdictService service = mock(MissionVerdictService.class);
        when(service.acknowledge(USER_ID, 123L)).thenReturn(new MissionVerdictAcknowledgeDto(
                123L, LocalDateTime.of(2026, 8, 14, 9, 10)));

        mockMvc(service).perform(post("/api/missions/verdicts/123/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentId").value(123))
                .andExpect(jsonPath("$.data.resultCheckedAt").value("2026-08-14T09:10:00"));
    }

    private MockMvc mockMvc(MissionVerdictService service) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders
                .standaloneSetup(new MissionVerdictController(service))
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
}

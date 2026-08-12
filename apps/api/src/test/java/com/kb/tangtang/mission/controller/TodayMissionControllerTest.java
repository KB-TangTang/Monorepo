package com.kb.tangtang.mission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.mapper.TodayMissionMapper;
import com.kb.tangtang.mission.service.TodayMissionService;
import org.junit.jupiter.api.DisplayName;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TodayMissionControllerTest {

    private static final long USER_ID = 7L;

    @Test
    @DisplayName("오늘 미션을 ApiResponse와 camelCase 필드로 반환한다")
    void returnsTodayMission() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TodayMissionController(new StubService()))
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        mockMvc.perform(get("/api/missions/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.missionId").value(11))
                .andExpect(jsonPath("$.data.missionType").value("RELATIVE"))
                .andExpect(jsonPath("$.data.assignDate").value("2026-08-12"))
                .andExpect(jsonPath("$.data.targetValue").value(18000));
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

    private static class StubService extends TodayMissionService {
        StubService() {
            super((TodayMissionMapper) null);
        }

        @Override
        public TodayMissionDto getTodayMission(long userId) {
            TodayMissionDto mission = new TodayMissionDto();
            mission.setMissionId(11L);
            mission.setMissionTitle("배달 현장 급습");
            mission.setMissionType("RELATIVE");
            mission.setAssignDate(LocalDate.of(2026, 8, 12));
            mission.setTargetValue(new BigDecimal("18000.00"));
            return mission;
        }
    }
}

package com.kb.tangtang.mission.controller;

import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import com.kb.tangtang.mission.service.MissionScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MissionScoreControllerTest {

    @Test
    void returnsCurrentMonthlyScoreWithApiResponse() throws Exception {
        MissionScoreService service = new MissionScoreService((MissionScoreMapper) null) {
            @Override
            public MissionMonthlyScoreDto getCurrentScore(long userId) {
                return new MissionMonthlyScoreDto("2026-08", 75);
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionScoreController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/missions/monthly-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-08"))
                .andExpect(jsonPath("$.data.totalScore").value(75));
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
                return 7L;
            }
        };
    }
}

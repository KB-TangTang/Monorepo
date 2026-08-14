package com.kb.tangtang.mission.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.service.DevMissionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevMissionControllerTest {

    @Test
    void returnsReassignedTodayMission() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DevMissionController(new StubService()))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(post("/api/dev/missions/today/reassign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.missionId").value(101));
    }

    private HandlerMethodArgumentResolver loginUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return 7L;
            }
        };
    }

    private static class StubService extends DevMissionService {
        StubService() {
            super(null, null, null, "local", null);
        }

        @Override
        public TodayMissionDto reassignTodayMission(long userId) {
            TodayMissionDto mission = new TodayMissionDto();
            mission.setMissionId(101L);
            return mission;
        }
    }
}

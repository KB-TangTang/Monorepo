package com.kb.tangtang.mission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.dto.MissionCategoryRankDto;
import com.kb.tangtang.mission.service.MissionAnalysisSnapshotService;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MissionCategoryAnalysisControllerTest {

    private static final long USER_ID = 1L;

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

    private static class StubService extends MissionAnalysisSnapshotService {
        StubService() {
            super(null, null);
        }

        @Override
        public MissionCategoryAnalysisDto getCurrentRotationAnalysis(long userId) {
            return MissionCategoryAnalysisDto.builder()
                    .analysisStartDate(LocalDate.of(2026, 7, 14))
                    .analysisEndDate(LocalDate.of(2026, 8, 10))
                    .transactionCount(63)
                    .relativeEligible(true)
                    .topCategories(List.of(MissionCategoryRankDto.builder()
                            .rank(1)
                            .categoryId(17L)
                            .parentCategoryName("식비")
                            .categoryName("배달앱")
                            .totalAmount(new BigDecimal("150000"))
                            .transactionCount(10)
                            .spendingRatio(new BigDecimal("30.00"))
                            .rotationAssignDate(LocalDate.of(2026, 8, 10))
                            .rotationResult("SUCCESS")
                            .missionRound(3)
                            .build()))
                    .build();
        }
    }

    @Test
    @DisplayName("카테고리 분석 응답은 ApiResponse와 camelCase 필드로 반환한다")
    void returnsCamelCaseResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionCategoryAnalysisController(new StubService()))
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        mockMvc.perform(get("/api/missions/categoryAnalysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analysisStartDate").value("2026-07-14"))
                .andExpect(jsonPath("$.data.analysisEndDate").value("2026-08-10"))
                .andExpect(jsonPath("$.data.transactionCount").value(63))
                .andExpect(jsonPath("$.data.relativeEligible").value(true))
                .andExpect(jsonPath("$.data.topCategories[0].categoryId").value(17))
                .andExpect(jsonPath("$.data.topCategories[0].categoryName").value("배달앱"))
                .andExpect(jsonPath("$.data.topCategories[0].spendingRatio").value(30.00))
                .andExpect(jsonPath("$.data.topCategories[0].rotationAssignDate").value("2026-08-10"))
                .andExpect(jsonPath("$.data.topCategories[0].rotationResult").value("SUCCESS"))
                .andExpect(jsonPath("$.data.topCategories[0].missionRound").value(3));
    }
}

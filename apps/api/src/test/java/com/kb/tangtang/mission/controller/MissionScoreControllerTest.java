package com.kb.tangtang.mission.controller;

import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.dto.MissionCertificateDto;
import com.kb.tangtang.mission.dto.MissionCertificateTitlesDto;
import com.kb.tangtang.mission.dto.MissionMonthlyRankingDto;
import com.kb.tangtang.mission.dto.MissionMyRankingDto;
import com.kb.tangtang.mission.dto.MissionRankingEntryDto;
import com.kb.tangtang.mission.dto.MissionRankingMonthsDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import com.kb.tangtang.mission.service.MissionScoreService;
import com.kb.tangtang.mission.service.MissionCertificateTitleService;
import org.junit.jupiter.api.Test;
import java.util.List;
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
                return new MissionMonthlyScoreDto("2026-08", 75, 15);
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
                .andExpect(jsonPath("$.data.totalScore").value(75))
                .andExpect(jsonPath("$.data.topPercent").value(15));
    }

    @Test
    void returnsMonthlyRankingWithApiResponse() throws Exception {
        MissionScoreService service = new MissionScoreService((MissionScoreMapper) null) {
            @Override
            public MissionMonthlyRankingDto getMonthlyRanking(long userId, String yearMonth) {
                return new MissionMonthlyRankingDto(
                        "2026-07",
                        100,
                        List.of(new MissionRankingEntryDto(1, 12L, "서영", null, 1340)),
                        new MissionMyRankingDto(12, "나", "/images/me.png", 480, 12)
                );
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionScoreController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/missions/rankings").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-07"))
                .andExpect(jsonPath("$.data.topRankings[0].nickname").value("서영"))
                .andExpect(jsonPath("$.data.myRanking.topPercent").value(12));
    }

    @Test
    void returnsMonthsThatHaveRankingData() throws Exception {
        MissionScoreService service = new MissionScoreService((MissionScoreMapper) null) {
            @Override
            public MissionRankingMonthsDto getRankingMonths() {
                return new MissionRankingMonthsDto(List.of("2026-08", "2025-12"));
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionScoreController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/missions/rankings/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yearMonths[0]").value("2026-08"))
                .andExpect(jsonPath("$.data.yearMonths[1]").value("2025-12"));
    }

    @Test
    void returnsCertificateWithApiResponse() throws Exception {
        MissionScoreService service = new MissionScoreService((MissionScoreMapper) null) {
            @Override
            public MissionCertificateDto getCertificate(long userId, String yearMonth) {
                return new MissionCertificateDto(
                        "2026-07", 100, new MissionMyRankingDto(12, "나", null, 480, 12),
                        5, 9, 31, 25);
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionScoreController(service))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/missions/rankings/certificate").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bestStreakDays").value(9))
                .andExpect(jsonPath("$.data.myRanking.topPercent").value(12));
    }

    @Test
    void returnsCertificateAiTitlesWithApiResponse() throws Exception {
        MissionScoreService scoreService = new MissionScoreService((MissionScoreMapper) null);
        MissionCertificateTitleService titleService = new MissionCertificateTitleService(
                null, null, null, null, null, "") {
            @Override
            public MissionCertificateTitlesDto getTitles(long userId, String yearMonth) {
                return new MissionCertificateTitlesDto(
                        "2026-07", List.of("상위 12% 절약 판결관", "7일 연속 무죄의 지배자", "소비 법정 MVP"), "OPENAI");
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MissionScoreController(scoreService, titleService))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/missions/rankings/certificate/titles").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.source").value("OPENAI"))
                .andExpect(jsonPath("$.data.titles[1]").value("7일 연속 무죄의 지배자"));
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

package com.kb.tangtang.report.controller;

import com.kb.tangtang.report.dto.ChallengeReportMonthDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import com.kb.tangtang.report.service.ChallengeReportService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChallengeReportControllerTest {

    private static final long USER_ID = 7L;

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

    private static class StubService extends ChallengeReportService {
        StubService() {
            super((ChallengeReportMapper) null);
        }

        @Override
        public ChallengeReportMonthsDto getAvailableMonths(long userId) {
            return ChallengeReportMonthsDto.builder()
                    .entryState("READY")
                    .months(List.of(ChallengeReportMonthDto.builder()
                            .value("2026-07")
                            .year(2026)
                            .month(7)
                            .available(true)
                            .hasReport(true)
                            .firstReport(true)
                            .status("READY")
                            .build()))
                    .build();
        }
    }

    @Test
    void returnsAvailableChallengeReportMonthsInApiResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChallengeReportController(new StubService()))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mockMvc.perform(get("/api/reports/challenge/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entryState").value("READY"))
                .andExpect(jsonPath("$.data.months[0].value").value("2026-07"))
                .andExpect(jsonPath("$.data.months[0].firstReport").value(true));
    }
}

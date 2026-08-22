package com.kb.tangtang.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.user.domain.TutorialType;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 튜토리얼 완료·다시보기 (이슈 #128).
 * 경로가 화면 도메인(main-challenge / group-challenge)이라 개인·그룹이 서로 뒤바뀌지 않는지 본다.
 */
@ExtendWith(MockitoExtension.class)
class TutorialControllerTest {

    private static final long USER_ID = 7L;
    private static final LocalDateTime SEEN = LocalDateTime.of(2026, 8, 11, 10, 0);

    @Mock private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver loginUser = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }
            @Override
            public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          org.springframework.web.context.request.NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return USER_ID;
            }
        };

        // RootConfig.objectMapper() 와 동일하게 맞춘다 — 그래야 시각이 ISO-8601 문자열로 나가는지
        // 이 테스트가 실제 운영 직렬화를 검증한다. 프론트는 이 문자열의 null 여부만 본다.
        ObjectMapper responseObjectMapper = new ObjectMapper();
        responseObjectMapper.registerModule(new JavaTimeModule());
        responseObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new TutorialController(userService))
                .setCustomArgumentResolvers(loginUser)
                .setControllerAdvice(new CommonExceptionAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(responseObjectMapper))
                .build();
    }

    private static UserMeDto me(LocalDateTime main, LocalDateTime group) {
        return UserMeDto.builder()
                .id(USER_ID)
                .nickname("지윤")
                .tutorialSeenAt(main)
                .groupTutorialSeenAt(group)
                .build();
    }

    @Test
    @DisplayName("메인 튜토리얼 완료 — MAIN 으로 서비스를 부르고 시각을 ISO 문자열로 돌려준다")
    void completeMain() throws Exception {
        when(userService.markTutorialSeen(USER_ID, TutorialType.MAIN)).thenReturn(me(SEEN, null));

        mockMvc.perform(patch("/api/main-challenge/tutorial/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tutorialSeenAt").value("2026-08-11T10:00:00"))
                .andExpect(jsonPath("$.data.groupTutorialSeenAt").isEmpty());
    }

    @Test
    @DisplayName("메인 다시 보기 — 완료 시각이 null 로 비워진다")
    void resetMain() throws Exception {
        when(userService.clearTutorialSeen(USER_ID, TutorialType.MAIN)).thenReturn(me(null, SEEN));

        mockMvc.perform(delete("/api/main-challenge/tutorial/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorialSeenAt").isEmpty())
                .andExpect(jsonPath("$.data.groupTutorialSeenAt").value("2026-08-11T10:00:00"));
    }

    @Test
    @DisplayName("그룹 튜토리얼 완료 — GROUP 으로 부른다. 개인 것과 뒤바뀌면 안 된다")
    void completeGroup() throws Exception {
        when(userService.markTutorialSeen(USER_ID, TutorialType.GROUP)).thenReturn(me(null, SEEN));

        mockMvc.perform(patch("/api/group-challenge/tutorial/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupTutorialSeenAt").value("2026-08-11T10:00:00"));
    }

    @Test
    @DisplayName("그룹 다시 보기")
    void resetGroup() throws Exception {
        when(userService.clearTutorialSeen(USER_ID, TutorialType.GROUP)).thenReturn(me(SEEN, null));

        mockMvc.perform(delete("/api/group-challenge/tutorial/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupTutorialSeenAt").isEmpty());
    }
}

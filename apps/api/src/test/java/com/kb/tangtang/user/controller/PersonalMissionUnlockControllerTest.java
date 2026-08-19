package com.kb.tangtang.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.user.domain.PersonalMissionUnlockStatus;
import com.kb.tangtang.user.dto.PersonalMissionUnlockDto;
import com.kb.tangtang.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PersonalMissionUnlockControllerTest {

    private static final long USER_ID = 7L;

    @Mock private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver loginUser = new TestLoginUserArgumentResolver(USER_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(new PersonalMissionUnlockController(userService))
                .setCustomArgumentResolvers(loginUser)
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    @Test
    void syncReturnsPendingUnlock() throws Exception {
        when(userService.syncPersonalMissionUnlock(USER_ID, true)).thenReturn(
                PersonalMissionUnlockDto.from(PersonalMissionUnlockStatus.PENDING));

        mockMvc.perform(post("/api/main-challenge/mission-unlock/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of("enoughData", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.showUnlock").value(true));
    }

    @Test
    void acknowledgeReturnsSeen() throws Exception {
        when(userService.acknowledgePersonalMissionUnlock(USER_ID)).thenReturn(
                PersonalMissionUnlockDto.from(PersonalMissionUnlockStatus.SEEN));

        mockMvc.perform(patch("/api/main-challenge/mission-unlock/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SEEN"))
                .andExpect(jsonPath("$.data.showUnlock").value(false));
    }

    private static final class TestLoginUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final long userId;

        private TestLoginUserArgumentResolver(long userId) {
            this.userId = userId;
        }

        @Override
        public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
            return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
        }

        @Override
        public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                      org.springframework.web.context.request.NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return userId;
        }
    }
}

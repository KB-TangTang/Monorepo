package com.kb.tangtang.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final long USER_ID = 7L;

    @Mock private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * standaloneSetup 에는 LoginUserArgumentResolver 가 없다.
     * 인터셉터도 타지 않으므로 @LoginUser 자리에 고정 사용자 ID 를 넣어주는 리졸버를 직접 단다.
     */
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

        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, new ImageProcessor()))
                .setCustomArgumentResolvers(loginUser)
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private static UserMeDto me(String name) {
        return UserMeDto.builder()
                .id(USER_ID)
                .nickname("지윤")
                .socialName("JH Jang")
                .displayName("지윤")
                .name(name)
                .email("me@example.com")
                .socialProvider("GOOGLE")
                .build();
    }

    @Test
    @DisplayName("본인 정보에 실명이 함께 실린다")
    void me() throws Exception {
        when(userService.me(USER_ID)).thenReturn(me("장재한"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("장재한"))
                .andExpect(jsonPath("$.data.nickname").value("지윤"));
    }

    @Test
    @DisplayName("PATCH 는 로그인 사용자 ID 로 실명을 저장하고 갱신된 정보를 돌려준다")
    void updateName() throws Exception {
        when(userService.updateName(USER_ID, "장재한")).thenReturn(me("장재한"));

        String body = objectMapper.writeValueAsString(Map.of("name", "장재한"));

        mockMvc.perform(patch("/api/users/me/name")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("장재한"));
    }

    @Test
    @DisplayName("닉네임 PATCH 는 로그인 사용자 ID 로 저장하고 표시명까지 돌려준다")
    void updateNickname() throws Exception {
        when(userService.updateNickname(USER_ID, "탕탕이")).thenReturn(me("장재한"));

        String body = objectMapper.writeValueAsString(Map.of("nickname", "탕탕이"));

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("지윤"))
                .andExpect(jsonPath("$.data.socialName").value("JH Jang"))
                .andExpect(jsonPath("$.data.displayName").value("지윤"));
    }

    @Test
    @DisplayName("빈 닉네임은 400 + INVALID_REQUEST 로 나간다")
    void updateNicknameInvalid() throws Exception {
        when(userService.updateNickname(USER_ID, "  "))
                .thenThrow(new BusinessException("INVALID_REQUEST", "닉네임을 입력해 주세요."));

        String body = objectMapper.writeValueAsString(Map.of("nickname", "  "));

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("형식이 어긋난 이름은 400 + INVALID_NAME 으로 나간다")
    void updateNameInvalid() throws Exception {
        when(userService.updateName(USER_ID, "장재한1"))
                .thenThrow(new BusinessException("INVALID_NAME", "이름은 한글 또는 영문으로 입력해 주세요."));

        String body = objectMapper.writeValueAsString(Map.of("name", "장재한1"));

        mockMvc.perform(patch("/api/users/me/name")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_NAME"));
    }

    /* ── 프로필 이미지 (이슈 #150) ───────────────────────── */

    @Test
    @DisplayName("프로필 이미지를 올리면 갱신된 사용자 정보를 돌려준다")
    void uploadsProfileImage() throws Exception {
        when(userService.updateProfileImage(eq(USER_ID), any())).thenReturn(me("장재한"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/users/me/profile-image")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("파일 파트가 비면 400 이다")
    void rejectsEmptyFile() throws Exception {
        when(userService.updateProfileImage(eq(USER_ID), any()))
                .thenThrow(new BusinessException("IMAGE_REQUIRED", "올릴 이미지를 선택해주세요."));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/users/me/profile-image")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "a.jpg", "image/jpeg", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMAGE_REQUIRED"));
    }

    @Test
    @DisplayName("프로필 이미지를 지우면 갱신된 사용자 정보를 돌려준다")
    void deletesProfileImage() throws Exception {
        when(userService.deleteProfileImage(USER_ID)).thenReturn(me("장재한"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/users/me/profile-image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

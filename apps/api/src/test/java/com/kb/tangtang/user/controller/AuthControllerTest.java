package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.AuthCookieWriter;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.service.AuthService;
import com.kb.tangtang.user.service.GoogleOAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String FRONT_URL = "http://localhost:5173";

    @Mock private AuthService authService;
    @Mock private GoogleOAuthClient googleOAuthClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthCookieWriter cookieWriter = new AuthCookieWriter("Lax", false, 1209600);
        AuthController controller = new AuthController(authService, googleOAuthClient, cookieWriter, FRONT_URL);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private static AuthResultDto authResult() {
        return AuthResultDto.builder()
                .response(LoginResponseDto.builder()
                        .accessToken("access-jwt")
                        .user(UserMeDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com").build())
                        .needsConsent(true)
                        .build())
                .refreshToken("refresh-raw")
                .build();
    }

    @Test
    @DisplayName("GET /api/auth/google 은 구글로 302 하고 state 쿠키를 심는다")
    void googleRedirect() throws Exception {
        when(googleOAuthClient.buildAuthorizationUrl(anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=x");

        MvcResult result = mockMvc.perform(get("/api/auth/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals("https://accounts.google.com/o/oauth2/v2/auth?state=x",
                result.getResponse().getRedirectedUrl());
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("oauth_state="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("콜백이 성공하면 리프레시 쿠키를 심고 프론트 /auth/callback 으로 보낸다")
    void callbackSuccess() throws Exception {
        when(authService.loginWithGoogleCode("the-code")).thenReturn(authResult());

        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/auth/callback", result.getResponse().getRedirectedUrl());
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                        .anyMatch(header -> header.startsWith("refresh_token=refresh-raw")),
                "리프레시 토큰이 쿠키로 나가야 한다");
        assertTrue(result.getResponse().getRedirectedUrl().indexOf("refresh-raw") < 0,
                "토큰이 URL 에 노출되면 안 된다");
    }

    @Test
    @DisplayName("state 가 쿠키와 다르면 로그인 화면으로 error=invalid 를 붙여 되돌린다")
    void callbackStateMismatch() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "attacker-state")
                        .cookie(new Cookie("oauth_state", "real-state")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=invalid", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("state 쿠키가 아예 없어도 로그인 화면으로 되돌린다")
    void callbackMissingStateCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=invalid", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("사용자가 구글에서 취소하면 error=cancelled 로 되돌린다")
    void callbackUserCancelled() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("error", "access_denied")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=cancelled", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("로그인 처리 중 업무 예외가 나면 코드에 맞는 쿼리로 되돌린다")
    void callbackBusinessError() throws Exception {
        when(authService.loginWithGoogleCode("the-code"))
                .thenThrow(new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다."));

        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=withdrawn", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("POST /api/auth/refresh 는 새 액세스 토큰과 사용자 정보를 준다")
    void refresh() throws Exception {
        when(authService.refresh("old-raw")).thenReturn(authResult());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "old-raw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.user.nickname").value("지윤"))
                .andExpect(jsonPath("$.data.needsConsent").value(true));
    }

    @Test
    @DisplayName("쿠키 없이 refresh 하면 400 과 INVALID_TOKEN 이다")
    void refreshWithoutCookie() throws Exception {
        // 컨트롤러는 쿠키가 없으면 null 을 넘긴다. 실제 AuthService 안에서는
        // RefreshTokenService.consume(null) 이 INVALID_TOKEN 을 던지므로 목도 같게 흉내낸다.
        // 이 스텁이 없으면 목이 null 을 돌려주고 다음 줄에서 NPE → 500 이 되어 테스트가 깨진다.
        when(authService.refresh(null))
                .thenThrow(new BusinessException("INVALID_TOKEN", "리프레시 토큰이 없습니다."));

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃은 토큰을 폐기하고 쿠키를 만료시킨다")
    void logout() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", "raw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        verify(authService).logout("raw");
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                        .anyMatch(header -> header.startsWith("refresh_token=") && header.contains("Max-Age=0")),
                "쿠키를 만료시켜야 한다");
    }
}

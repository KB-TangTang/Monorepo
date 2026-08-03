package com.kb.tangtang.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthInterceptorTest {

    private static final String SECRET = "tangtang-test-secret-key-must-be-32-bytes-or-longer";

    private JwtProvider jwtProvider;
    private JwtAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 900);
        interceptor = new JwtAuthInterceptor(jwtProvider, new ObjectMapper());
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 통과시키고 userId 를 request 에 넣는다")
    void validToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(42L, request.getAttribute(JwtAuthInterceptor.LOGIN_USER_ID));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401 과 UNAUTHORIZED 를 준다")
    void missingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Bearer 접두가 없으면 401 을 준다")
    void malformedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Token abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("만료된 토큰은 401 과 TOKEN_EXPIRED 를 준다")
    void expiredToken() throws Exception {
        String expired = new JwtProvider(SECRET, -60).createAccessToken(42L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + expired);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("CORS preflight(OPTIONS)는 검사하지 않고 통과시킨다")
    void preflightPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("401 응답은 UTF-8 JSON 이다")
    void errorResponseEncoding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}

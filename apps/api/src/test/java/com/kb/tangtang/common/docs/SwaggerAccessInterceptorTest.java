package com.kb.tangtang.common.docs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 배포 환경 Swagger 문서 차단 (이슈 #225).
 *
 * <p>2026-08-14 점검에서 배포 서버의 {@code /swagger-ui.html} 이 무인증 200 이었다.
 * 여기서 지키는 것은 <b>"설정을 빠뜨려도 열리지 않는다"</b> 는 성질이다 —
 * 비밀번호를 안 넣었을 때 통과시키는 구현이면 조치가 있으나 마나다.
 */
class SwaggerAccessInterceptorTest {

    private static final String USER = "tangtang";
    private static final String PASS = "team-shared-secret";

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/swagger-ui.html");
    }

    private String basic(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("로컬은 비밀번호 설정과 무관하게 통과한다 — 개발 중에 매번 로그인하지 않는다")
    void localPasses() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("local", "", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request(), response, new Object()));
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("배포 환경에서 비밀번호가 비어 있으면 404 로 숨긴다 — 설정 누락의 기본값은 '닫힘' 이다")
    void missingPasswordHides() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", "", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request(), response, new Object()));
        assertEquals(404, response.getStatus());
    }

    @Test
    @DisplayName("배포 환경에서 자격 증명이 맞으면 통과한다")
    void validCredentialsPass() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic(USER, PASS));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401 과 WWW-Authenticate 를 준다 — 브라우저가 로그인 창을 띄운다")
    void missingHeaderChallenges() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request(), response, new Object()));
        assertEquals(401, response.getStatus());
        assertNotNull(response.getHeader("WWW-Authenticate"));
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic realm="));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 이다")
    void wrongPasswordRejected() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic(USER, "wrong"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("아이디가 틀리면 401 이다")
    void wrongUsernameRejected() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic("attacker", PASS));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("Basic 이 아닌 헤더(Bearer)로는 통과하지 못한다")
    void nonBasicSchemeRejected() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer " + PASS);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("Base64 가 깨진 헤더는 예외 없이 401 이다 — 잘못된 입력으로 500 을 내지 않는다")
    void malformedBase64Rejected() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Basic !!!not-base64!!!");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("콜론이 없는 본문은 401 이다")
    void missingColonRejected() {
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, PASS);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Basic "
                + Base64.getEncoder().encodeToString("nocolon".getBytes(StandardCharsets.UTF_8)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("비밀번호에 콜론이 들어 있어도 첫 콜론만 구분자로 본다")
    void passwordWithColon() {
        String passWithColon = "a:b:c";
        SwaggerAccessInterceptor interceptor = new SwaggerAccessInterceptor("docker", USER, passWithColon);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic(USER, passWithColon));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}

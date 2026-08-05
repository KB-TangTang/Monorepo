package com.kb.tangtang.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 네트워크를 타지 않는다. MockRestServiceServer 로 구글 토큰 엔드포인트를 흉내낸다.
 */
class GoogleOAuthClientTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String REDIRECT_URI = "http://localhost:5173/api/auth/google/callback";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GoogleOAuthClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new GoogleOAuthClient(restTemplate, new ObjectMapper(),
                "test-client-id", "test-client-secret", REDIRECT_URI, AUTH_URI, TOKEN_URI);
    }

    /** 구글 id_token 흉내 — 헤더.페이로드.서명 세 토막. 페이로드만 base64url 로 읽는다. */
    private static String fakeIdToken(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fake-signature";
    }

    @Test
    @DisplayName("authorization URL 에 필수 파라미터가 모두 들어간다")
    void buildAuthorizationUrl() {
        String url = client.buildAuthorizationUrl("state-abc");

        assertTrue(url.startsWith(AUTH_URI + "?"), "구글 인가 엔드포인트로 시작해야 한다");
        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state=state-abc"));
        assertTrue(url.contains("scope=openid+email+profile")
                        || url.contains("scope=openid%20email%20profile"),
                "openid email profile 스코프가 있어야 한다");
        assertTrue(url.contains("redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Fapi%2Fauth%2Fgoogle%2Fcallback"),
                "redirect_uri 는 URL 인코딩돼야 한다");
    }

    @Test
    @DisplayName("code 를 교환해 id_token 에서 프로필을 뽑는다")
    void exchangeCodeForProfile() {
        String idToken = fakeIdToken(
                "{\"sub\":\"1234567890\",\"email\":\"jiyoon@example.com\",\"name\":\"지윤\"}");
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"id_token\":\"" + idToken + "\"}",
                        MediaType.APPLICATION_JSON));

        GoogleProfileDto profile = client.exchangeCodeForProfile("auth-code");

        assertEquals("1234567890", profile.getProviderUserId());
        assertEquals("jiyoon@example.com", profile.getEmail());
        assertEquals("지윤", profile.getName());
        mockServer.verify();
    }

    @Test
    @DisplayName("구글이 오류를 주면 OAUTH_TOKEN_EXCHANGE_FAILED 로 바꾼다")
    void exchangeFails() {
        mockServer.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("bad-code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }

    @Test
    @DisplayName("id_token 이 없는 응답도 OAUTH_TOKEN_EXCHANGE_FAILED 로 바꾼다")
    void missingIdToken() {
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"only-access\"}", MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }

    @Test
    @DisplayName("sub 가 없는 id_token 은 거부한다")
    void missingSub() {
        String idToken = fakeIdToken("{\"email\":\"no-sub@example.com\"}");
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"id_token\":\"" + idToken + "\"}", MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }
}

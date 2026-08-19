package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class TossAuthClientTest {

    private static final String TOKEN_URL = "https://openapi.tossinvest.com/oauth2/token";

    @Test
    @DisplayName("client_id/client_secret/grant_type을 폼으로 실어 토큰을 발급받는다")
    void fetchesTokenWithFormBody() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                /*
                 * contentType(완전일치) 이면 안 된다 — FormHttpMessageConverter 가 charset 을 덧붙여
                 * 실제 헤더는 application/x-www-form-urlencoded;charset=UTF-8 로 나간다.
                 * 여기서 검증할 것은 "폼 인코딩으로 보냈는가" 이지 charset 유무가 아니다.
                 */
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(
                        "client_id=id-1&client_secret=secret-1&grant_type=client_credentials"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-abc\",\"token_type\":\"Bearer\",\"expires_in\":86400}",
                        MediaType.APPLICATION_JSON));

        TossAuthClient client = new TossAuthClient(restTemplate, "id-1", "secret-1");
        TossAccessToken token = client.fetchToken();

        assertEquals("tok-abc", token.getAccessToken());
        assertEquals(86400L, token.getExpiresInSeconds());
        server.verify();
    }

    @Test
    @DisplayName("client_id/client_secret이 비어 있으면 호출하지 않고 즉시 실패한다")
    void failsFastWhenCredentialsMissing() {
        TossAuthClient client = new TossAuthClient(new RestTemplate(), "", "secret-1");

        assertThrows(BusinessException.class, client::fetchToken);
    }

    @Test
    @DisplayName("토스 서버가 401을 주면 BusinessException으로 바뀌고, 메시지에 상태 코드가 남는다")
    void wrapsUnauthorizedAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(TOKEN_URL)).andRespond(withUnauthorizedRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"INVALID_CLIENT\",\"message\":\"잘못된 client_id/client_secret\"}}"));

        TossAuthClient client = new TossAuthClient(restTemplate, "id-1", "wrong-secret");

        /*
         * 이 테스트가 QA 디버깅에서 확인한 진짜 버그를 잠근다 — 예전엔 401(인증 실패)과 진짜
         * 네트워크 연결 실패가 똑같은 "연결하지 못했어요" 메시지로 뭉뚱그려져서 원인을 못 가렸다.
         * 이제는 상태 코드가 메시지에 남아 401 인 걸 로그만 보고도 알 수 있어야 한다.
         */
        BusinessException e = assertThrows(BusinessException.class, client::fetchToken);
        assertTrue(e.getMessage().contains("401"), "401 상태 코드가 메시지에 남아야 진짜 원인(인증 실패)을 알 수 있다");
    }

    @Test
    @DisplayName("토스 서버가 500을 주면 BusinessException으로 바뀌고, 메시지에 상태 코드가 남는다")
    void wrapsServerErrorAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

        TossAuthClient client = new TossAuthClient(restTemplate, "id-1", "secret-1");

        BusinessException e = assertThrows(BusinessException.class, client::fetchToken);
        assertTrue(e.getMessage().contains("500"), "500 상태 코드가 메시지에 남아야 인증 실패와 구분된다");
    }
}

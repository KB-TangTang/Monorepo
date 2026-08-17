package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
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
    @DisplayName("토스 서버가 401을 주면 BusinessException으로 바뀐다")
    void wrapsUnauthorizedAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(TOKEN_URL)).andRespond(withUnauthorizedRequest());

        TossAuthClient client = new TossAuthClient(restTemplate, "id-1", "wrong-secret");

        assertThrows(BusinessException.class, client::fetchToken);
    }

    @Test
    @DisplayName("토스 서버가 500을 주면 BusinessException으로 바뀐다")
    void wrapsServerErrorAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

        TossAuthClient client = new TossAuthClient(restTemplate, "id-1", "secret-1");

        assertThrows(BusinessException.class, client::fetchToken);
    }
}

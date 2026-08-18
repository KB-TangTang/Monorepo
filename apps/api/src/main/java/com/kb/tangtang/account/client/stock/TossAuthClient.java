package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 토스증권 Open API 액세스 토큰 발급 (OAuth2 Client Credentials Grant).
 *
 * ⚠ 토스 문서: "client 당 유효한 access token 은 1개. 재발급 시 이전 token 은 즉시 무효화된다."
 *   그래서 이 클라이언트는 필요할 때마다 부르는 게 아니라 {@link TossAuthScheduler} 가
 *   만료 전에 한 번만 갱신하고 {@link TossTokenHolder} 에 보관하는 방식으로 써야 한다 —
 *   다른 곳에서 fetchToken() 을 또 부르면 그 순간 앞서 발급된 토큰이 무효화된다.
 *
 * ⚠ CODEF(CodefFinancialDataClient.accessToken())와 달리 매 호출마다 새로 받지 않는다.
 *   CODEF 는 유효기간이 7일이라 매번 다시 받아도 부담이 없다고 문서화돼 있지만, 토스는
 *   AUTH 전용 레이트리밋 그룹이 따로 있어(문서 429 항목) 그 방식을 그대로 가져오면 안 된다.
 */
public class TossAuthClient {

    private static final Logger log = LoggerFactory.getLogger(TossAuthClient.class);
    private static final String TOKEN_URL = "https://openapi.tossinvest.com/oauth2/token";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    public TossAuthClient(RestTemplate restTemplate, String clientId, String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public TossAccessToken fetchToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "토스 연동 설정이 없어요. toss.client-id / toss.client-secret 을 확인해 주세요.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        /* 문서: 요청 본문은 application/x-www-form-urlencoded, client_id/client_secret/grant_type. */
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");

        Map<?, ?> response;
        try {
            response = restTemplate.postForObject(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);
        } catch (RestClientResponseException e) {
            /*
             * 서버가 실제로 응답은 줬지만 에러였을 때(401/403/429/5xx 등) — RestClientException
             * 하나로 뭉뚱그리면 "서버에 연결 자체가 안 됐다"는 것과 구분이 안 된다(QA 디버깅 중 발견).
             * status·본문을 로그로 남겨야 client-id/secret 오류인지, IP 미허용(403)인지,
             * 레이트리밋(429)인지 바로 알 수 있다 — 원인 예외를 삼키지 않고 로그에 남긴다.
             */
            log.error("토스 토큰 발급 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE",
                    "토스 인증에 실패했어요 (status=" + e.getRawStatusCode() + ").");
        } catch (RestClientException e) {
            /* 여기까지 오면 상태 코드조차 못 받은, 진짜 연결 실패(DNS·타임아웃·TLS 등)다. */
            log.error("토스 인증 서버에 연결하지 못했다", e);
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE", "토스 인증 서버에 연결하지 못했어요.");
        }

        String accessToken = response == null ? null : TossJsonSupport.string(response.get("access_token"));
        Long expiresIn = response == null ? null : number(response.get("expires_in"));
        if (accessToken == null || expiresIn == null) {
            throw new BusinessException("EXTERNAL_API_ERROR", "토스 토큰 응답을 읽지 못했어요.");
        }
        return TossAccessToken.builder()
                .accessToken(accessToken)
                .expiresInSeconds(expiresIn)
                .build();
    }

    private static Long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

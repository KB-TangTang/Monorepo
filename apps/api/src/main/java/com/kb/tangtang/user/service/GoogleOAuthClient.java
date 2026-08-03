package com.kb.tangtang.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 구글 OAuth 2.0 연동. 이 클래스만 구글과 통신한다.
 *
 * RestTemplate 은 생성자로 받는다 — 테스트에서 MockRestServiceServer 를 물리기 위해서다.
 */
@Component
@Log4j2
public class GoogleOAuthClient {

    private static final String SCOPE = "openid email profile";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizationUri;
    private final String tokenUri;

    public GoogleOAuthClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             @Value("${google.oauth.client-id}") String clientId,
                             @Value("${google.oauth.client-secret}") String clientSecret,
                             @Value("${google.oauth.redirect-uri}") String redirectUri,
                             @Value("${google.oauth.authorization-uri}") String authorizationUri,
                             @Value("${google.oauth.token-uri}") String tokenUri) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
    }

    /**
     * 사용자를 보낼 구글 동의 화면 주소. state 는 호출자가 만들어 쿠키에도 저장한다.
     *
     * UriComponentsBuilder#encode() 는 RFC 3986 상 query 컴포넌트에 이미 허용된 ':', '/' 는
     * 인코딩하지 않는다 — redirect_uri 값 안의 ':', '/' 가 그대로 남아 구글이 쿼리 파라미터
     * 경계를 오인식할 수 있다. URLEncoder 로 각 값을 직접 퍼센트 인코딩해 조립한다.
     */
    public String buildAuthorizationUrl(String state) {
        StringBuilder url = new StringBuilder(authorizationUri).append('?');
        url.append("client_id=").append(encode(clientId));
        url.append("&redirect_uri=").append(encode(redirectUri));
        url.append("&response_type=").append(encode("code"));
        url.append("&scope=").append(encode(SCOPE));
        url.append("&state=").append(encode(state));
        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * authorization code 를 구글 토큰 엔드포인트에서 교환하고 id_token 의 클레임을 돌려준다.
     *
     * id_token 의 서명은 검증하지 않는다. TLS 로 구글 토큰 엔드포인트와 직접 통신해 받은
     * 응답이므로 출처가 보장된다(OpenID Connect Core 3.1.3.7 — 코드 플로우에서 서버가
     * 직접 받은 id_token 은 서명 검증 생략이 허용된다). 프론트에서 받은 id_token 을
     * 검증 없이 신뢰하는 것과는 다른 상황이다.
     */
    public GoogleProfileDto exchangeCodeForProfile(String code) {
        String idToken = requestIdToken(code);
        return parseIdToken(idToken);
    }

    private String requestIdToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body;
        try {
            body = restTemplate.postForObject(tokenUri, new HttpEntity<>(form, headers), String.class);
        } catch (RestClientException ex) {
            log.warn("구글 토큰 교환 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode idToken = root.get("id_token");
            if (idToken == null || idToken.asText().isBlank()) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
            }
            return idToken.asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("구글 토큰 응답 파싱 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }
    }

    private GoogleProfileDto parseIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));

            JsonNode sub = claims.get("sub");
            if (sub == null || sub.asText().isBlank()) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 계정 식별자를 확인할 수 없습니다.");
            }

            return GoogleProfileDto.builder()
                    .providerUserId(sub.asText())
                    .email(claims.hasNonNull("email") ? claims.get("email").asText() : null)
                    .name(claims.hasNonNull("name") ? claims.get("name").asText() : null)
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("id_token 파싱 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }
    }
}

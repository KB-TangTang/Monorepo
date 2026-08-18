package com.kb.tangtang.account.client.stock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 토스증권 Open API 전용 설정.
 *
 * RootConfig 에 공용 RestTemplate 빈이 이미 있고(구글 OAuth 용), FinancialClientConfig·
 * FinancialSyncClientConfig 도 각자 전용 RestTemplate 을 따로 두는 것과 같은 이유로 여기도
 * 분리한다 — 같이 쓰면 NoUniqueBeanDefinitionException 이 난다.
 *
 * toss.client-id / toss.client-secret 이 비어 있어도 컨텍스트는 뜬다(openai.api-key 와 같은 방식) —
 * TossAuthClient.fetchToken() 이 호출 시점에 BusinessException 으로 막고, TossAuthScheduler 가
 * 그 예외를 로그로만 남기고 삼킨다. 토스 연동 없이도 나머지 기능은 그대로 동작해야 한다.
 */
@Configuration
public class TossStockClientConfig {

    @Value("${toss.client-id}")
    private String clientId;

    @Value("${toss.client-secret}")
    private String clientSecret;

    @Value("${toss.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${toss.read-timeout-ms}")
    private int readTimeoutMs;

    /**
     * ⚠ 타임아웃 없이 new RestTemplate() 만 쓰면 안 된다(QA 지적사항) — 기본 SimpleClientHttpRequestFactory
     *   는 커넥트·리드 타임아웃이 사실상 무한대라, 토스가 느려지거나 응답을 안 주면 이 호출을 문 스레드가
     *   무한정 붙잡힌다. InvestmentPriceRefresher.refresh() 가 이 클라이언트를 부르는 동안은 다른 요청들이
     *   같은 심볼의 갱신을 기다리므로(락 참고), 타임아웃이 없으면 그 대기도 무한정 늘어난다.
     */
    @Bean
    @Qualifier("tossRestTemplate")
    public RestTemplate tossRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    @Bean
    public TossAuthClient tossAuthClient(@Qualifier("tossRestTemplate") RestTemplate restTemplate) {
        return new TossAuthClient(restTemplate, clientId, clientSecret);
    }

    @Bean
    public TossPriceClient tossPriceClient(@Qualifier("tossRestTemplate") RestTemplate restTemplate,
                                           TossTokenHolder tokenHolder) {
        return new TossPriceClient(restTemplate, tokenHolder);
    }
}

package com.kb.tangtang.account.client.stock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${toss.client-id:}")
    private String clientId;

    @Value("${toss.client-secret:}")
    private String clientSecret;

    @Bean
    @Qualifier("tossRestTemplate")
    public RestTemplate tossRestTemplate() {
        return new RestTemplate();
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

package com.kb.tangtang.account.client.sync;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * 이슈 #147 금융 동기화 전용 RestTemplate.
 *
 * account.client.FinancialClientConfig 의 financialRestTemplate 과는 별개 빈이다(@Qualifier 로 구분) —
 * 그쪽은 타임아웃이 없고(계좌연동 흐름이라 사용자가 화면에서 기다린다), 이쪽은 새로고침 버튼 하나로
 * 13개 엔드포인트를 순차 호출하므로 커넥트/리드 타임아웃을 명시해야 한다.
 */
@Configuration
public class FinancialSyncClientConfig {

    @Value("${mock.server.base-url}")
    private String baseUrl;

    @Value("${mock.server.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${mock.server.read-timeout-ms}")
    private int readTimeoutMs;

    @Value("${mock.server.scenario-keys}")
    private String scenarioKeysRaw;

    @Bean
    @Qualifier("financialSyncRestTemplate")
    public RestTemplate financialSyncRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    @Bean
    public FinancialSyncClient financialSyncClient(
            @Qualifier("financialSyncRestTemplate") RestTemplate restTemplate) {
        return new MockFinancialSyncClient(restTemplate, baseUrl);
    }

    @Bean
    public ScenarioKeyProvider scenarioKeyProvider() {
        List<String> keys = Arrays.stream(scenarioKeysRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new PooledScenarioKeyProvider(keys);
    }
}

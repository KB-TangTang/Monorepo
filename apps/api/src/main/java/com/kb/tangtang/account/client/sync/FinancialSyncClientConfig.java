package com.kb.tangtang.account.client.sync;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

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

    /**
     * FinancialSyncServiceImpl.collectAll() 이 BANK/DEPOSIT/SECURITIES/LOAN/PAY_MONEY/CARD
     * 6개 소스를 병렬로 수집할 때 쓰는 전용 실행기다.
     *
     * RootConfig 의 taskExecutor(빈 이름 taskExecutor, @Async 전용)와 별개다 — 그쪽은 알림처럼
     * 짧고 던지고 잊는(fire-and-forget) 작업 전용이라 여기서 같이 쓰면 외부 API 를 기다리는 동안
     * 알림 발송이 큐에서 밀릴 수 있다. 풀 크기는 한 번의 sync() 가 만드는 태스크 수(6개)에 맞춘다 —
     * 그보다 키워봐야 목서버가 단일 인스턴스(도커 컨테이너 1개)라 동시 요청이 늘어날 뿐 더 빨라지지
     * 않는다.
     */
    @Bean
    public Executor financialSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("financial-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}

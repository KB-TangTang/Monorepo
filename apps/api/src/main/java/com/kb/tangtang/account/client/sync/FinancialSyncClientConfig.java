package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.user.mapper.UserMapper;
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

    /**
     * 시연용 유저는 tbl_user.mock_scenario_key 로 시나리오를 고정할 수 있다(UserOverridingScenarioKeyProvider).
     * 그 값이 없는 더미 유저는 기존 풀-나머지 로직(PooledScenarioKeyProvider)으로 폴백한다.
     */
    @Bean
    public ScenarioKeyProvider scenarioKeyProvider(UserMapper userMapper) {
        List<String> keys = Arrays.stream(scenarioKeysRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new UserOverridingScenarioKeyProvider(userMapper, new PooledScenarioKeyProvider(keys));
    }

    /**
     * FinancialSyncServiceImpl.collectAll() 이 BANK/DEPOSIT/SECURITIES/LOAN/PAY_MONEY/CARD
     * 6개 소스를 병렬로 수집할 때 쓰는 전용 실행기다.
     *
     * RootConfig 의 taskExecutor(빈 이름 taskExecutor, @Async 전용)와 별개다 — 그쪽은 알림처럼
     * 짧고 던지고 잊는(fire-and-forget) 작업 전용이라 여기서 같이 쓰면 외부 API 를 기다리는 동안
     * 알림 발송이 큐에서 밀릴 수 있다.
     *
     * ⚠ 풀 크기를 사용자 1명 몫(6)에 딱 맞추면 안 된다. FinancialSyncBatchScheduler 는 한 번에
     *   한 사용자씩 처리하지만(runBatch() 의 for 문이 순차), 그 한 사용자가 collectAll() 을 도는 동안
     *   6개를 전부 점유해 버려 — 그 순간 사용자가 새로고침 버튼을 눌러도 배치가 끝날 때까지 큐에서
     *   기다리게 된다. 12(배치 1명분 + 수동 1명분)로 잡아 이 완전 직렬화만 풀어준다.
     *   그렇다고 훨씬 크게 잡지는 않는다 — 목서버가 단일 인스턴스(도커 컨테이너 1개, 자기 DB도
     *   같은 MySQL 컨테이너를 공유)라 동시 요청을 늘려봐야 병목이 목서버 쪽 큐로 옮겨갈 뿐이고,
     *   커넥트 3초·리드 5초 타임아웃(위 필드)에 걸려 "느림"이 아니라 EXTERNAL_API_UNAVAILABLE 로
     *   실패할 위험만 커진다.
     */
    @Bean
    public Executor financialSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("financial-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}

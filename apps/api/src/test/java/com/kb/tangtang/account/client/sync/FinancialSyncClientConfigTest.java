package com.kb.tangtang.account.client.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FinancialSyncServiceImplTest 는 이 빈을 거치지 않고 서비스를 직접 new 하므로
 * (Runnable::run 이나 손으로 만든 스레드풀을 넘긴다), @Qualifier("financialSyncExecutor") 가
 * 실제로 이 클래스의 빈과 이어지는지는 그 테스트로는 증명되지 않는다 — 이름이 틀리거나
 * 타입이 안 맞아도 단위 테스트는 계속 통과한다. 여기서는 진짜 Spring 컨테이너로 이 설정
 * 클래스만 띄워 빈이 실제로 만들어지고 동작하는지 확인한다(RootContextWiringTest 와 같은
 * 동기 — "컴파일도 단위 테스트도 통과했는데 컨테이너에서만 깨지는" 부류의 사고를 잡는다).
 */
class FinancialSyncClientConfigTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context == null) {
            return;
        }
        Executor executor = context.getBean("financialSyncExecutor", Executor.class);
        if (executor instanceof ThreadPoolTaskExecutor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
        context.close();
    }

    @Test
    @DisplayName("financialSyncExecutor 빈이 실제로 뜨고, 병렬 수집이 기대하는 크기(6)로 고정돼 있다")
    void financialSyncExecutorBeanIsWiredAndBounded() {
        context = new AnnotationConfigApplicationContext();
        context.register(FinancialSyncClientConfig.class);

        PropertySourcesPlaceholderConfigurer placeholders = new PropertySourcesPlaceholderConfigurer();
        Properties props = new Properties();
        props.setProperty("mock.server.base-url", "http://localhost:8081");
        props.setProperty("mock.server.connect-timeout-ms", "3000");
        props.setProperty("mock.server.read-timeout-ms", "5000");
        props.setProperty("mock.server.scenario-keys", "1");
        placeholders.setProperties(props);
        context.addBeanFactoryPostProcessor(placeholders);

        context.refresh();

        Executor executor = context.getBean("financialSyncExecutor", Executor.class);
        assertTrue(executor instanceof ThreadPoolTaskExecutor, "ThreadPoolTaskExecutor 여야 풀 크기를 검증할 수 있다");
        ThreadPoolTaskExecutor pooled = (ThreadPoolTaskExecutor) executor;
        /* collectAll() 이 정확히 6개(BANK/DEPOSIT/SECURITIES/LOAN/PAY_MONEY/CARD) 태스크를 동시에 던진다.
           풀이 이보다 작으면 일부가 큐에서 대기해 병렬 효과가 줄고, 이보다 훨씬 크면 단일 목서버
           인스턴스에 필요 이상의 동시 요청을 보낸다. */
        assertEquals(6, pooled.getCorePoolSize());
        assertEquals(6, pooled.getMaxPoolSize());

        /* 빈이 존재하는 것만이 아니라 실제로 이 풀의 스레드에서 태스크를 실행하는지까지 확인한다. */
        AtomicReference<String> runnerThreadName = new AtomicReference<>();
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> runnerThreadName.set(Thread.currentThread().getName()), executor);
        future.join();
        assertTrue(runnerThreadName.get().startsWith("financial-sync-"),
                "financialSyncExecutor 로 실행됐다면 스레드 이름이 financial-sync- 로 시작해야 한다");
    }
}

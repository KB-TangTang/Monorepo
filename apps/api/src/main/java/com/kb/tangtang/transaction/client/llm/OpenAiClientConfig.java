package com.kb.tangtang.transaction.client.llm;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 이슈 #147 후속 — OpenAI 호출 전용 RestTemplate.
 *
 * RootConfig 의 공용 restTemplate(구글 OAuth 용)은 타임아웃이 없다. LLM 분류는 스케줄러가
 * 트랜잭션 안에서 부르는 블로킹 호출이라, 응답이 없으면 스케줄러 스레드와 DB 커넥션을 함께
 * 붙잡고 늘어진다. 그래서 FinancialSyncClientConfig 와 같은 방식으로 전용 빈을 따로 둔다
 * (공용 빈을 고치면 다른 클라이언트까지 영향을 받는다).
 */
@Configuration
public class OpenAiClientConfig {

    @Value("${openai.api.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${openai.api.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    @Qualifier("openAiRestTemplate")
    public RestTemplate openAiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}

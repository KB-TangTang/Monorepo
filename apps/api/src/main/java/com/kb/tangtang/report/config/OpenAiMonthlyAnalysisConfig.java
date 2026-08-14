package com.kb.tangtang.report.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** OpenAI 호출만을 위한 짧은 타임아웃의 RestTemplate 설정. */
@Configuration
public class OpenAiMonthlyAnalysisConfig {

    @Bean
    @Qualifier("openAiRestTemplate")
    public RestTemplate openAiRestTemplate(
            @Value("${openai.connect-timeout-millis:3000}") int connectTimeoutMillis,
            @Value("${openai.read-timeout-millis:15000}") int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        return new RestTemplate(requestFactory);
    }
}

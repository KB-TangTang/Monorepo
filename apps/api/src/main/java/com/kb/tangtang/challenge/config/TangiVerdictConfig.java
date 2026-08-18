package com.kb.tangtang.challenge.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 판사 탕이(동률 판결 LLM) 전용 {@link RestTemplate} (이슈 #172).
 *
 * <p><b>타임아웃이 이 설정의 전부다.</b> 기본 {@code RestTemplate} 은 무한 대기라, 응답이 오지
 * 않으면 개표 배치 스레드가 영영 잡힌 채 다음 틱이 돌지 못한다 — 동률 재판 한 건 때문에 그날의
 * 모든 재판이 멈춘다.
 *
 * <p>월간 리포트 분석({@code openAiRestTemplate})과 빈을 나눈 이유는 <b>호출 성격이 다르기
 * 때문</b>이다. 저쪽은 사용자가 화면 앞에서 기다리는 요청이고 이쪽은 스케줄러가 도는 배치다.
 * 한쪽 타임아웃을 조정하면 다른 쪽이 같이 흔들리는 것을 막는다. API 키는 공유한다.
 */
@Configuration
public class TangiVerdictConfig {

    @Bean
    @Qualifier("tangiVerdictRestTemplate")
    public RestTemplate tangiVerdictRestTemplate(
            @Value("${challenge.trial.ai-verdict.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${challenge.trial.ai-verdict.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }
}

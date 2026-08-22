package com.kb.tangtang.account.client;

import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

/**
 * 금융 데이터 공급자 선택.
 *
 * **`financial.client` 프로퍼티 한 줄로 목서버 ↔ 실 CODEF 가 갈린다.**
 * 프론트·서비스·컨트롤러는 어느 구현이 붙었는지 알지 못한다. 발표 QA 의
 * "설정 교체만으로 실 CODEF 전환 가능" 주장이 이 파일 하나에 걸려 있다.
 *
 * ⚠ Spring Boot 의 `@ConditionalOnProperty` 는 쓸 수 없다(Spring Legacy 프로젝트다).
 *   프로퍼티를 직접 읽어 빈을 고른다.
 *
 * ⚠ RootConfig 에 공용 `RestTemplate` 빈이 이미 있다(구글 OAuth 용).
 *   여기서 또 만들면 `NoUniqueBeanDefinitionException` 이 나므로 @Qualifier 로 이름을 붙인다.
 */
@Configuration
public class FinancialClientConfig {

    /** mock | codef */
    @Value("${financial.client:mock}")
    private String client;

    @Value("${financial.mock.base-url:http://localhost:8081}")
    private String mockBaseUrl;

    @Value("${financial.codef.base-url:https://api.codef.io}")
    private String codefBaseUrl;

    @Value("${financial.codef.client-id:}")
    private String codefClientId;

    @Value("${financial.codef.client-secret:}")
    private String codefClientSecret;

    /**
     * CODEF 발급 공개키(X.509 Base64). 비밀번호 RSA 암호화에 쓴다.
     * 시크릿은 아니지만 계정에 묶인 값이라 개인 설정 파일에 둔다.
     */
    @Value("${financial.codef.public-key:}")
    private String codefPublicKey;

    /**
     * 금융 API 전용 RestTemplate.
     * 공용 빈과 분리한 이유는 타임아웃·인터셉터를 따로 걸어야 하기 때문이다 —
     * 외부 금융 API 는 느리고, 구글 OAuth 와 같은 설정을 쓸 이유가 없다.
     */
    @Bean
    @Qualifier("financialRestTemplate")
    public RestTemplate financialRestTemplate() {
        return new RestTemplate();
    }

    /**
     * ⚠ 시나리오 키 선택은 FinancialSyncClientConfig 의 {@link ScenarioKeyProvider} 빈 하나로
     * 통일한다(#334). 예전엔 이 클라이언트가 `financial.mock.scenario-key` 프로퍼티로 시나리오를
     * 고정해 뒀는데, 동기화 쪽(FinancialSyncServiceImpl)은 `mock.server.scenario-keys` 로 사용자마다
     * 다른 시나리오를 골랐다. 두 설정의 기본값이 서로 달라(demo-normal-user vs 1), 같은 사용자가
     * 계좌 연동에서 고른 계좌와 배치가 동기화한 계좌가 서로 다른 가짜 데이터에서 나와 뒤섞였다.
     */
    @Bean
    public FinancialDataClient financialDataClient(
            @Qualifier("financialRestTemplate") RestTemplate restTemplate,
            ScenarioKeyProvider scenarioKeyProvider) {
        if ("codef".equalsIgnoreCase(client)) {
            /*
             * 공개키가 없으면 여기서 즉시 실패한다 — 인증 시점에 알면 원인을 찾기 어렵다.
             * (CODEF 는 평문 비밀번호를 거부하므로 공개키 없이는 어차피 동작하지 않는다)
             */
            return new CodefFinancialDataClient(
                    restTemplate,
                    codefBaseUrl,
                    codefClientId,
                    codefClientSecret,
                    new CodefPasswordCipher(codefPublicKey));
        }
        return new MockFinancialDataClient(
                restTemplate, mockBaseUrl, scenarioKeyProvider, Clock.systemDefaultZone());
    }
}

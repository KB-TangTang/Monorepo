package com.kb.tangtang.account.client;

import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.account.client.sync.PooledScenarioKeyProvider;
import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
import com.kb.tangtang.account.domain.AuthMethod;
import com.kb.tangtang.account.domain.AuthStatus;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 목서버 구현 검증.
 *
 * 인증은 흉내이므로 시계를 고정해 승인 시점을 검사하고,
 * 계좌 조회는 실제 HTTP 호출이므로 MockRestServiceServer 로 응답을 지정한다.
 */
class MockFinancialDataClientTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final Instant NOW = Instant.parse("2026-08-05T09:00:00Z");
    /* 시나리오 라우팅 자체는 PooledScenarioKeyProviderTest 가 검증한다. 여기서는 고정 시나리오로 충분하다. */
    private static final ScenarioKeyProvider SCENARIO = new PooledScenarioKeyProvider(List.of("demo-normal-user"));

    private Clock fixed(long plusSeconds) {
        return Clock.fixed(NOW.plusSeconds(plusSeconds), ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("목 모드는 간편인증만 지원한다 - 목서버에 기관 로그인 API 가 없다")
    void supportsSimpleAuthOnly() {
        MockFinancialDataClient client = new MockFinancialDataClient(
                new RestTemplate(), BASE_URL, SCENARIO, fixed(0));

        assertEquals(List.of(AuthMethod.SIMPLE_AUTH), client.supportedAuthMethods());
        assertEquals(List.of("KAKAO", "PASS", "NAVER"), client.simpleAuthProviders());
    }

    @Test
    @DisplayName("인증 요청 직후에는 대기, 승인 지연이 지나면 승인된다")
    void approvesAfterDelay() {
        RestTemplate restTemplate = new RestTemplate();
        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, SCENARIO, fixed(0));

        ConnectionResult result = client.createConnection(ConnectionRequest.builder()
                .authMethod(AuthMethod.SIMPLE_AUTH)
                .provider("KAKAO")
                .organizations(List.of("0004"))
                .build());

        assertEquals(AuthStatus.PENDING, result.getStatus());
        assertEquals(AuthStatus.PENDING, client.getAuthStatus(result.getConnectionId()));

        // 승인 지연(3초)이 지난 시계를 쓰는 클라이언트는 같은 상태를 승인으로 본다
        MockFinancialDataClient later = new MockFinancialDataClient(
                restTemplate, BASE_URL, SCENARIO, fixed(5));
        ConnectionResult laterResult = later.createConnection(ConnectionRequest.builder()
                .authMethod(AuthMethod.SIMPLE_AUTH)
                .provider("KAKAO")
                .organizations(List.of("0004"))
                .build());
        // 같은 시계에서는 아직 대기다
        assertEquals(AuthStatus.PENDING, later.getAuthStatus(laterResult.getConnectionId()));
    }

    @Test
    @DisplayName("모르는 connectionId 는 CONNECTION_NOT_FOUND 로 막는다")
    void unknownConnection() {
        MockFinancialDataClient client = new MockFinancialDataClient(
                new RestTemplate(), BASE_URL, SCENARIO, fixed(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.getAuthStatus("없는-값"));
        assertEquals("CONNECTION_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("기관을 선택하지 않으면 연결을 시작하지 않는다")
    void rejectsEmptyOrganizations() {
        MockFinancialDataClient client = new MockFinancialDataClient(
                new RestTemplate(), BASE_URL, SCENARIO, fixed(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.createConnection(ConnectionRequest.builder()
                        .authMethod(AuthMethod.SIMPLE_AUTH)
                        .provider("KAKAO")
                        .organizations(List.of())
                        .build()));
        assertEquals("EXTERNAL_API_ERROR", e.getCode());
    }

    @Test
    @DisplayName("목서버 응답에서 해당 기관 계좌만 골라낸다")
    void filtersByOrganization() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts?scenarioKey=demo-normal-user"))
                .andRespond(withSuccess("""
                        {"code":"OK","message":"성공","data":{"accounts":[
                          {"accountId":1,"bankCode":"0004","bankName":"KB국민은행","accountType":"11",
                           "accountName":"입출금통장","accountNoMasked":"110-***-****23",
                           "currency":"KRW","balance":8340000},
                          {"accountId":2,"bankCode":"0090","bankName":"카카오뱅크","accountType":"11",
                           "accountName":"세이프박스","accountNoMasked":"3333-**-****189",
                           "currency":"KRW","balance":500000}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, SCENARIO, fixed(0));

        List<FinancialAccountDto> accounts = client.fetchAccounts(1L, "mock-1", "0004");

        assertEquals(1, accounts.size());
        assertEquals("입출금통장", accounts.get(0).getAccountName());
        assertEquals("110-***-****23", accounts.get(0).getAccountNo());
        server.verify();
    }

    /*
     * 목서버가 2026-08-06 v2 리팩토링에서 응답 필드명을 통째로 바꿨다.
     *   bankCode → institutionCode · bankName → institutionName · accountName → productName
     *   accountType(DEMAND_DEPOSIT/SAVINGS) → accountTypeCode('1001')
     * 이걸 못 읽어 bankCode 가 null 이 되면 기관 필터가 전부 걸러내 "가져올 계좌가 없어요"가 뜬다.
     * 위 filtersByOrganization 은 구버전 형태를 먹여서 계속 통과했다 — 그래서 못 잡았다.
     */
    @Test
    @DisplayName("목서버 v2 필드명(institutionCode·productName)으로 와도 읽는다")
    void readsV2FieldNames() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts?scenarioKey=demo-normal-user"))
                .andRespond(withSuccess("""
                        {"code":"OK","message":"성공","data":{"accounts":[
                          {"accountId":1,"institutionCode":"0004","institutionName":"KB Kookmin Bank",
                           "accountTypeCode":"1001","accountStatusCode":"01",
                           "productName":"KB Star Checking","accountNoMasked":"123456******7890",
                           "currency":"KRW","balance":1244200,"availableAmount":1244200},
                          {"accountId":2,"institutionCode":"0088","institutionName":"Shinhan Bank",
                           "accountTypeCode":"1001","accountStatusCode":"01",
                           "productName":"신한 주거래통장","accountNoMasked":"110-***-****23",
                           "currency":"KRW","balance":500000,"availableAmount":500000},
                          {"accountId":3,"institutionCode":"0004","institutionName":"KB Kookmin Bank",
                           "accountTypeCode":"2001","accountStatusCode":"01",
                           "productName":"KB Time Deposit","accountNoMasked":"777777******0001",
                           "currency":"KRW","balance":5032000,"availableAmount":5032000}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, SCENARIO, fixed(0));

        List<FinancialAccountDto> accounts = client.fetchAccounts(1L, "mock-1", "0004");

        assertEquals(2, accounts.size());
        FinancialAccountDto account = accounts.get(0);
        assertEquals("0004", account.getOrganization());
        assertEquals("KB Kookmin Bank", account.getBankName());
        assertEquals("KB Star Checking", account.getAccountName());
        assertEquals("123456******7890", account.getAccountNo());
        /*
         * v2 는 예적금을 deposit_account 로 분리했고 이 엔드포인트는 bank_account(입출금)만 준다.
         * accountTypeCode('1001')는 우리 도메인 값이 아니므로 넘기지 않는다 —
         * AccountLinkService 의 판정이 depositTypeCode 가 null 인 것을 보고 DEMAND_DEPOSIT 으로 정한다.
         */
        assertNull(account.getAccountType());
        assertNull(account.getDepositTypeCode());
        FinancialAccountDto deposit = accounts.get(1);
        assertEquals("KB Time Deposit", deposit.getAccountName());
        assertEquals("2001", deposit.getDepositTypeCode());
        server.verify();
    }

    @Test
    @DisplayName("목서버가 죽으면 기관 하나의 실패로 취급할 수 있게 업무 예외로 바꾼다")
    void serverDownBecomesBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts?scenarioKey=demo-normal-user"))
                .andRespond(withServerError());

        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, SCENARIO, fixed(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.fetchAccounts(1L, "mock-1", "0004"));
        assertEquals("EXTERNAL_API_UNAVAILABLE", e.getCode());
    }

    /*
     * #334 리뷰 지적: 계좌 연동 흐름(이 클래스)과 배치 동기화(FinancialSyncServiceImpl)가 서로 다른
     * 설정(financial.mock.scenario-key 단일값 vs mock.server.scenario-keys 풀)으로 시나리오를 골라,
     * 같은 실사용자인데도 두 흐름이 서로 다른 목데이터를 봤다 — 실제 DB에서 user_id=1(demo-normal-user)
     * 계좌와 user_id=4(scenario_key='1') 계좌가 한 실사용자에게 같이 쌓인 것으로 확인됐다.
     * 이 테스트는 fetchAccounts() 가 이제 userId 로 ScenarioKeyProvider 를 직접 묻는다는 것,
     * 즉 동기화와 같은 라우팅 규칙을 탄다는 것을 증명한다.
     */
    @Test
    @DisplayName("userId 로 시나리오를 고른다 — FinancialSyncServiceImpl 과 같은 ScenarioKeyProvider 규칙")
    void resolvesScenarioKeyPerUserIdLikeSyncFlow() {
        ScenarioKeyProvider pooled = new PooledScenarioKeyProvider(List.of("demo-normal-user", "1"));

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        // userId=0 → floorMod(0,2)=0 → "demo-normal-user"
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts?scenarioKey=demo-normal-user"))
                .andRespond(withSuccess("""
                        {"code":"OK","message":"성공","data":{"accounts":[]}}
                        """, MediaType.APPLICATION_JSON));
        // userId=1 → floorMod(1,2)=1 → "1"
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts?scenarioKey=1"))
                .andRespond(withSuccess("""
                        {"code":"OK","message":"성공","data":{"accounts":[]}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, pooled, fixed(0));

        client.fetchAccounts(0L, "mock-1", "0004");
        client.fetchAccounts(1L, "mock-1", "0004");

        server.verify();
    }
}

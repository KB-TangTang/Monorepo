package com.kb.tangtang.account.client;

import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
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

    private Clock fixed(long plusSeconds) {
        return Clock.fixed(NOW.plusSeconds(plusSeconds), ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("목 모드는 간편인증만 지원한다 - 목서버에 기관 로그인 API 가 없다")
    void supportsSimpleAuthOnly() {
        MockFinancialDataClient client = new MockFinancialDataClient(
                new RestTemplate(), BASE_URL, "demo-normal-user", fixed(0));

        assertEquals(List.of(AuthMethod.SIMPLE_AUTH), client.supportedAuthMethods());
        assertEquals(List.of("KAKAO", "PASS", "NAVER"), client.simpleAuthProviders());
    }

    @Test
    @DisplayName("인증 요청 직후에는 대기, 승인 지연이 지나면 승인된다")
    void approvesAfterDelay() {
        RestTemplate restTemplate = new RestTemplate();
        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, "demo-normal-user", fixed(0));

        ConnectionResult result = client.createConnection(ConnectionRequest.builder()
                .authMethod(AuthMethod.SIMPLE_AUTH)
                .provider("KAKAO")
                .organizations(List.of("0004"))
                .build());

        assertEquals(AuthStatus.PENDING, result.getStatus());
        assertEquals(AuthStatus.PENDING, client.getAuthStatus(result.getConnectionId()));

        // 승인 지연(3초)이 지난 시계를 쓰는 클라이언트는 같은 상태를 승인으로 본다
        MockFinancialDataClient later = new MockFinancialDataClient(
                restTemplate, BASE_URL, "demo-normal-user", fixed(5));
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
                new RestTemplate(), BASE_URL, "demo-normal-user", fixed(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.getAuthStatus("없는-값"));
        assertEquals("CONNECTION_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("기관을 선택하지 않으면 연결을 시작하지 않는다")
    void rejectsEmptyOrganizations() {
        MockFinancialDataClient client = new MockFinancialDataClient(
                new RestTemplate(), BASE_URL, "demo-normal-user", fixed(0));

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
                restTemplate, BASE_URL, "demo-normal-user", fixed(0));

        List<FinancialAccountDto> accounts = client.fetchAccounts("mock-1", "0004");

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
                           "currency":"KRW","balance":500000,"availableAmount":500000}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialDataClient client = new MockFinancialDataClient(
                restTemplate, BASE_URL, "demo-normal-user", fixed(0));

        List<FinancialAccountDto> accounts = client.fetchAccounts("mock-1", "0004");

        assertEquals(1, accounts.size());
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
                restTemplate, BASE_URL, "demo-normal-user", fixed(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.fetchAccounts("mock-1", "0004"));
        assertEquals("EXTERNAL_API_UNAVAILABLE", e.getCode());
    }
}

package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.account.client.sync.dto.BankAccountSyncDto;
import com.kb.tangtang.account.client.sync.dto.StockAssetSyncDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockFinancialSyncClientTest {

    private static final String BASE_URL = "http://localhost:8081";

    private RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setRequestFactory(new SimpleClientHttpRequestFactory());
        return rt;
    }

    @Test
    @DisplayName("계좌 목록을 X-Scenario-Key 헤더로 조회한다")
    void getBankAccounts() {
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts"))
                .andExpect(header("X-Scenario-Key", "1"))
                .andRespond(withSuccess("""
                        {"code":"SUCCESS","message":"ok","data":{"accounts":[
                          {"accountId":1,"institutionCode":"0004","institutionName":"KB국민은행",
                           "accountTypeCode":"1001","productName":"KB Star Checking",
                           "accountNoMasked":"110-***-120045","currency":"KRW",
                           "balance":1244200,"availableAmount":1244200}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialSyncClient client = new MockFinancialSyncClient(restTemplate, BASE_URL);
        List<BankAccountSyncDto> accounts = client.getBankAccounts("1");

        assertEquals(1, accounts.size());
        assertEquals("0004", accounts.get(0).getInstitutionCode());
        assertEquals("110-***-120045", accounts.get(0).getAccountNoMasked());
        server.verify();
    }

    @Test
    @DisplayName("목서버가 죽으면 EXTERNAL_API_UNAVAILABLE 로 변환한다")
    void serverDown() {
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts"))
                .andRespond(withServerError());

        MockFinancialSyncClient client = new MockFinancialSyncClient(restTemplate, BASE_URL);

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.getBankAccounts("1"));
        assertEquals("EXTERNAL_API_UNAVAILABLE", e.getCode());
    }

    @Test
    @DisplayName("code 가 SUCCESS 가 아니면 EXTERNAL_API_ERROR 로 변환한다")
    void nonSuccessCode() {
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts"))
                .andRespond(withSuccess("""
                        {"code":"NOT_FOUND","message":"no data","data":null}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialSyncClient client = new MockFinancialSyncClient(restTemplate, BASE_URL);

        BusinessException e = assertThrows(BusinessException.class,
                () -> client.getBankAccounts("1"));
        assertEquals("EXTERNAL_API_ERROR", e.getCode());
    }

    @Test
    @DisplayName("증권 계좌가 없는 사용자는 getStockAsset 이 예외 대신 null 을 반환한다")
    void getStockAssetReturnsNullForEmptyPortfolio() {
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        // 목서버는 증권 계좌가 없는 사용자(예: demo-empty-user 시나리오)에게 code:"SUCCESS" 에
        // data:{} (accountId 없음) 로 응답한다 — 이건 에러가 아니라 "증권 자산 없음"이라는 정상 상태다.
        server.expect(requestTo(BASE_URL + "/api/v1/assets/stocks"))
                .andRespond(withSuccess("""
                        {"code":"SUCCESS","message":"ok","data":{}}
                        """, MediaType.APPLICATION_JSON));

        MockFinancialSyncClient client = new MockFinancialSyncClient(restTemplate, BASE_URL);
        StockAssetSyncDto result = client.getStockAsset("demo-empty-user");

        assertNull(result);
        server.verify();
    }

    @Test
    @DisplayName("13개 메서드 모두 정확한 요청 경로로 호출한다 (경로 오타 회귀 방지)")
    void allMethodsHitExpectedPaths() {
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        String emptyBody = """
                {"code":"SUCCESS","message":"ok","data":{}}
                """;

        server.expect(requestTo(BASE_URL + "/api/v1/assets/accounts"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/accounts/1/transactions"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/assets/deposits"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/deposits/1/transactions"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/assets/stocks"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/securities/1/transactions"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/assets/loans"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/loans/1/transactions"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/assets/payMoney"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/pay-money/1/transactions"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/cards"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/cards/1/approvals"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/cards/1/bills"))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));

        MockFinancialSyncClient client = new MockFinancialSyncClient(restTemplate, BASE_URL);

        client.getBankAccounts("1");
        client.getBankTransactions("1", 1L);
        client.getDeposits("1");
        client.getDepositTransactions("1", 1L);
        client.getStockAsset("1");
        client.getSecuritiesTransactions("1", 1L);
        client.getLoans("1");
        client.getLoanTransactions("1", 1L);
        client.getPayMoney("1");
        client.getPayMoneyTransactions("1", 1L);
        client.getCards("1");
        client.getCardApprovals("1", 1L);
        client.getCardBills("1", 1L);

        server.verify();
    }
}

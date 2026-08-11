package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.account.client.sync.dto.BankAccountSyncDto;
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
}

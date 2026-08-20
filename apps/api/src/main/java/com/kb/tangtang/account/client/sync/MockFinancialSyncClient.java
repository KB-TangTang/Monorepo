package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.account.client.sync.dto.BankAccountSyncDto;
import com.kb.tangtang.account.client.sync.dto.BankTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardApprovalSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardBillSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardSyncDto;
import com.kb.tangtang.account.client.sync.dto.DepositSyncDto;
import com.kb.tangtang.account.client.sync.dto.DepositTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.LoanSyncDto;
import com.kb.tangtang.account.client.sync.dto.LoanTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.PayMoneySyncDto;
import com.kb.tangtang.account.client.sync.dto.PayMoneyTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.SecuritiesTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.StockAssetSyncDto;
import com.kb.tangtang.account.client.sync.dto.StockHoldingSyncDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 이슈 #147 금융 동기화 전용 목서버 클라이언트.
 * 사용자 식별은 X-Scenario-Key 헤더 하나다 (쿼리 파라미터도 목서버가 받지만, 이 레포는 헤더로 통일한다).
 */
public class MockFinancialSyncClient implements FinancialSyncClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MockFinancialSyncClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<BankAccountSyncDto> getBankAccounts(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/assets/accounts", scenarioKey);
        List<BankAccountSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("accounts"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(BankAccountSyncDto.builder()
                    .accountId(longVal(row.get("accountId")))
                    .institutionCode(text(row.get("institutionCode")))
                    .institutionName(text(row.get("institutionName")))
                    .accountTypeCode(text(row.get("accountTypeCode")))
                    .productName(text(row.get("productName")))
                    .accountNoMasked(text(row.get("accountNoMasked")))
                    .currency(text(row.get("currency")))
                    .balance(decimal(row.get("balance")))
                    .availableAmount(decimal(row.get("availableAmount")))
                    .build());
        }
        return result;
    }

    @Override
    public List<BankTransactionSyncDto> getBankTransactions(String scenarioKey, long accountId) {
        return getBankTransactions(scenarioKey, accountId, null);
    }

    @Override
    public List<BankTransactionSyncDto> getBankTransactions(String scenarioKey, long accountId, String yearMonth) {
        Map<String, Object> data = get("/api/v1/accounts/" + accountId + "/transactions", scenarioKey, yearMonth);
        List<BankTransactionSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("transactions"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(BankTransactionSyncDto.builder()
                    .transactionId(longVal(row.get("transactionId")))
                    .transactedAt(text(row.get("transactedAt")))
                    .transTypeCode(text(row.get("transTypeCode")))
                    .amount(decimal(row.get("amount")))
                    .balanceAfter(row.get("balanceAfter") == null ? null : decimal(row.get("balanceAfter")))
                    .description(text(row.get("description")))
                    .rawJson(rawJsonText(row.get("rawJson")))
                    .build());
        }
        return result;
    }

    @Override
    public List<DepositSyncDto> getDeposits(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/assets/deposits", scenarioKey);
        List<DepositSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("deposits"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(DepositSyncDto.builder()
                    .depositAccountId(longVal(row.get("depositAccountId")))
                    .institutionCode(text(row.get("institutionCode")))
                    .institutionName(text(row.get("institutionName")))
                    .productName(text(row.get("productName")))
                    .accountNoMasked(text(row.get("accountNoMasked")))
                    .currency(text(row.get("currency")))
                    .principal(decimal(row.get("principal")))
                    .balance(decimal(row.get("balance")))
                    .interestRate(decimal(row.get("interestRate")))
                    .maturityDate(text(row.get("maturityDate")))
                    .build());
        }
        return result;
    }

    @Override
    public List<DepositTransactionSyncDto> getDepositTransactions(String scenarioKey, long depositAccountId) {
        Map<String, Object> data = get("/api/v1/deposits/" + depositAccountId + "/transactions", scenarioKey);
        List<DepositTransactionSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("transactions"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(DepositTransactionSyncDto.builder()
                    .transactionId(longVal(row.get("transactionId")))
                    .transactedAt(text(row.get("transactedAt")))
                    .transTypeCode(text(row.get("transTypeCode")))
                    .amount(decimal(row.get("amount")))
                    .balanceAfter(row.get("balanceAfter") == null ? null : decimal(row.get("balanceAfter")))
                    .description(text(row.get("description")))
                    .rawJson(rawJsonText(row.get("rawJson")))
                    .build());
        }
        return result;
    }

    @Override
    public StockAssetSyncDto getStockAsset(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/assets/stocks", scenarioKey);
        /*
         * 증권 계좌가 없는 사용자는 목서버가 code:"SUCCESS" 에 data:{} (accountId 없음) 로 응답한다
         * (MockAssetServiceImpl.emptyStockAsset(), demo-empty-user 시나리오). 이건 에러가 아니라
         * "증권 자산 없음" 이라는 정상 상태이므로 예외 대신 null 을 반환해 호출자가
         * if (stock != null) 으로 건너뛸 수 있게 한다.
         */
        if (data.get("accountId") == null) {
            return null;
        }
        List<StockHoldingSyncDto> holdings = new ArrayList<>();
        for (Object item : asList(data.get("holdings"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            holdings.add(StockHoldingSyncDto.builder()
                    .productCode(text(row.get("productCode")))
                    .productName(text(row.get("productName")))
                    .quantity(decimal(row.get("quantity")))
                    .averagePurchasePrice(decimal(row.get("averagePurchasePrice")))
                    .lastPrice(decimal(row.get("lastPrice")))
                    .purchaseAmount(decimal(row.get("purchaseAmount")))
                    .marketValue(decimal(row.get("marketValue")))
                    .profitLossAmount(decimal(row.get("profitLossAmount")))
                    .profitLossRate(decimal(row.get("profitLossRate")))
                    .build());
        }
        return StockAssetSyncDto.builder()
                .accountId(longVal(data.get("accountId")))
                .institutionCode(text(data.get("institutionCode")))
                .institutionName(text(data.get("institutionName")))
                .accountName(text(data.get("accountName")))
                .accountNoMasked(text(data.get("accountNoMasked")))
                .currency(text(data.get("currency")))
                .cashBalance(decimal(data.get("cashBalance")))
                .totalMarketValue(decimal(data.get("totalMarketValue")))
                .holdings(holdings)
                .build();
    }

    @Override
    public List<SecuritiesTransactionSyncDto> getSecuritiesTransactions(String scenarioKey, long accountId) {
        Map<String, Object> data = get("/api/v1/securities/" + accountId + "/transactions", scenarioKey);
        List<SecuritiesTransactionSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("transactions"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(SecuritiesTransactionSyncDto.builder()
                    .transactionId(longVal(row.get("transactionId")))
                    .transactedAt(text(row.get("transactedAt")))
                    .transTypeCode(text(row.get("transTypeCode")))
                    .securityProductCode(text(row.get("securityProductCode")))
                    .securityProductName(text(row.get("securityProductName")))
                    .quantity(decimal(row.get("quantity")))
                    .unitPrice(decimal(row.get("unitPrice")))
                    .transactionAmount(decimal(row.get("transactionAmount")))
                    .build());
        }
        return result;
    }

    @Override
    public List<LoanSyncDto> getLoans(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/assets/loans", scenarioKey);
        List<LoanSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("loans"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(LoanSyncDto.builder()
                    .loanId(longVal(row.get("loanId")))
                    .institutionCode(text(row.get("institutionCode")))
                    .institutionName(text(row.get("institutionName")))
                    .productName(text(row.get("productName")))
                    .loanNoMasked(text(row.get("loanNoMasked")))
                    .principal(decimal(row.get("principal")))
                    .balance(decimal(row.get("balance")))
                    .interestRate(decimal(row.get("interestRate")))
                    .startDate(text(row.get("startDate")))
                    .maturityDate(text(row.get("maturityDate")))
                    .monthlyPayment(decimal(row.get("monthlyPayment")))
                    .nextPaymentDate(text(row.get("nextPaymentDate")))
                    .build());
        }
        return result;
    }

    @Override
    public List<LoanTransactionSyncDto> getLoanTransactions(String scenarioKey, long loanId) {
        Map<String, Object> data = get("/api/v1/loans/" + loanId + "/transactions", scenarioKey);
        List<LoanTransactionSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("transactions"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(LoanTransactionSyncDto.builder()
                    .transactionId(longVal(row.get("transactionId")))
                    .transactedAt(text(row.get("transactedAt")))
                    .transTypeCode(text(row.get("transTypeCode")))
                    .amount(decimal(row.get("amount")))
                    .principalAmount(decimal(row.get("principalAmount")))
                    .interestAmount(decimal(row.get("interestAmount")))
                    .balanceAfter(row.get("balanceAfter") == null ? null : decimal(row.get("balanceAfter")))
                    .description(text(row.get("description")))
                    .rawJson(rawJsonText(row.get("rawJson")))
                    .build());
        }
        return result;
    }

    @Override
    public List<PayMoneySyncDto> getPayMoney(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/assets/payMoney", scenarioKey);
        List<PayMoneySyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("payMoney"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(PayMoneySyncDto.builder()
                    .payMoneyId(longVal(row.get("payMoneyId")))
                    .providerCode(text(row.get("providerCode")))
                    .providerName(text(row.get("providerName")))
                    .walletName(text(row.get("walletName")))
                    .balance(decimal(row.get("balance")))
                    .availableAmount(decimal(row.get("availableAmount")))
                    .pointAmount(decimal(row.get("pointAmount")))
                    .build());
        }
        return result;
    }

    @Override
    public List<PayMoneyTransactionSyncDto> getPayMoneyTransactions(String scenarioKey, long payMoneyId) {
        Map<String, Object> data = get("/api/v1/pay-money/" + payMoneyId + "/transactions", scenarioKey);
        List<PayMoneyTransactionSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("transactions"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(PayMoneyTransactionSyncDto.builder()
                    .transactionId(longVal(row.get("transactionId")))
                    .transactedAt(text(row.get("transactedAt")))
                    .transTypeCode(text(row.get("transTypeCode")))
                    .amount(decimal(row.get("amount")))
                    .balanceAfter(row.get("balanceAfter") == null ? null : decimal(row.get("balanceAfter")))
                    .merchantName(text(row.get("merchantName")))
                    .merchantCategoryCode(text(row.get("merchantCategoryCode")))
                    .merchantCategoryName(text(row.get("merchantCategoryName")))
                    .description(text(row.get("description")))
                    .rawJson(rawJsonText(row.get("rawJson")))
                    .build());
        }
        return result;
    }

    @Override
    public List<CardSyncDto> getCards(String scenarioKey) {
        Map<String, Object> data = get("/api/v1/cards", scenarioKey);
        List<CardSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("cards"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(CardSyncDto.builder()
                    .cardId(longVal(row.get("cardId")))
                    .institutionCode(text(row.get("institutionCode")))
                    .institutionName(text(row.get("institutionName")))
                    .cardNoMasked(text(row.get("cardNoMasked")))
                    .productName(text(row.get("productName")))
                    .cardProductCode(text(row.get("cardProductCode")))
                    .cardTypeCode(text(row.get("cardTypeCode")))
                    .cardStatusCode(text(row.get("cardStatusCode")))
                    .currency(text(row.get("currency")))
                    .issuedAt(text(row.get("issuedAt")))
                    .build());
        }
        return result;
    }

    @Override
    public List<CardApprovalSyncDto> getCardApprovals(String scenarioKey, long cardId) {
        return getCardApprovals(scenarioKey, cardId, null);
    }

    @Override
    public List<CardApprovalSyncDto> getCardApprovals(String scenarioKey, long cardId, String yearMonth) {
        Map<String, Object> data = get("/api/v1/cards/" + cardId + "/approvals", scenarioKey, yearMonth);
        List<CardApprovalSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("approvals"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(CardApprovalSyncDto.builder()
                    .approvalId(longVal(row.get("approvalId")))
                    .approvalNo(text(row.get("approvalNo")))
                    .approvedAt(text(row.get("approvedAt")))
                    .approvalTypeCode(text(row.get("approvalTypeCode")))
                    .merchantName(text(row.get("merchantName")))
                    .merchantCategoryCode(text(row.get("merchantCategoryCode")))
                    .merchantCategoryName(text(row.get("merchantCategoryName")))
                    .approvedAmount(decimal(row.get("approvedAmount")))
                    .rawJson(rawJsonText(row.get("rawJson")))
                    .build());
        }
        return result;
    }

    @Override
    public List<CardBillSyncDto> getCardBills(String scenarioKey, long cardId) {
        Map<String, Object> data = get("/api/v1/cards/" + cardId + "/bills", scenarioKey);
        List<CardBillSyncDto> result = new ArrayList<>();
        for (Object item : asList(data.get("bills"))) {
            Map<?, ?> row = (Map<?, ?>) item;
            result.add(CardBillSyncDto.builder()
                    .billId(longVal(row.get("billId")))
                    .billingMonth(text(row.get("billingMonth")))
                    .dueDate(text(row.get("dueDate")))
                    .billStatusCode(text(row.get("billStatusCode")))
                    .billStatusName(text(row.get("billStatusName")))
                    .totalAmount(decimal(row.get("totalAmount")))
                    .paidAmount(decimal(row.get("paidAmount")))
                    .build());
        }
        return result;
    }

    /**
     * 공통 GET + envelope 해석.
     * 목서버 공통 envelope: {code, message, data, traceId, timestamp}. code!="SUCCESS" 또는
     * data 가 Map 이 아니면 EXTERNAL_API_ERROR — 응답은 왔지만 우리가 쓸 수 없는 형태라는 뜻이다.
     * RestClientException(네트워크 단절, 타임아웃, 4xx/5xx) 은 EXTERNAL_API_UNAVAILABLE 로 구분한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String scenarioKey) {
        return get(path, scenarioKey, null);
    }

    /**
     * yearMonth 가 있으면 쿼리 파라미터로 붙인다 — BANK/CARD 증분 수집 전용(이슈 #199).
     * 나머지는 기존 get(path, scenarioKey)와 동일한 envelope 해석 로직이다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String scenarioKey, String yearMonth) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (yearMonth != null && !yearMonth.isBlank()) {
            builder.queryParam("yearMonth", yearMonth);
        }
        String url = builder.toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Scenario-Key", scenarioKey);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        } catch (RestClientException e) {
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE",
                    "목서버 연결에 실패했어요 (" + path + "). 잠시 후 다시 시도해 주세요.");
        }

        Map<String, Object> envelope = response.getBody();
        if (envelope == null || !"SUCCESS".equals(envelope.get("code"))) {
            String message = envelope == null ? "empty body" : String.valueOf(envelope.get("message"));
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "목서버 응답 오류 (" + path + "): " + message);
        }
        Object data = envelope.get("data");
        if (!(data instanceof Map)) {
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "목서버 응답 형식이 올바르지 않아요 (" + path + ").");
        }
        return (Map<String, Object>) data;
    }

    private static List<?> asList(Object value) {
        return value instanceof List ? (List<?>) value : List.of();
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longVal(Object value) {
        if (value == null) {
            throw new BusinessException("EXTERNAL_API_ERROR", "목서버 응답에 식별자가 없어요.");
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "목서버 응답의 식별자 형식이 올바르지 않아요: " + value);
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** rawJson 은 응답 안에서 이미 파싱된 Map/List 로 올 수도, 문자열로 올 수도 있다 — 문자열로 통일한다. */
    private static String rawJsonText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

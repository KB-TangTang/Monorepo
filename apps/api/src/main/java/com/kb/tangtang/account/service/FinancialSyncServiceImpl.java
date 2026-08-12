package com.kb.tangtang.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.account.client.sync.FinancialSyncClient;
import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
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
import com.kb.tangtang.account.domain.Card;
import com.kb.tangtang.account.domain.CardBill;
import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.account.domain.FinancialSyncHistory;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.domain.Loan;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.mapper.CardBillMapper;
import com.kb.tangtang.account.mapper.CardMapper;
import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import com.kb.tangtang.account.mapper.FinancialSyncHistoryMapper;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이슈 #147 — 금융 동기화 오케스트레이터.
 *
 * 외부 목서버 호출(collectAll)은 전부 트랜잭션 밖에서 수행한다 — AccountLinkService.refresh() 와
 * 같은 이유로, 여러 건의 외부 HTTP 호출 동안 DB 커넥션을 붙잡으면 커넥션 풀이 마른다.
 * 전부 성공한 뒤에만 saveAll() 로 DB 에 반영한다. 하나라도 실패하면 DB 는 손대지 않고
 * 실패 이력만 남긴 뒤 BusinessException 을 그대로 올린다(설계 문서 §8).
 *
 * 저장은 전부 "자연키로 update 시도 → 0행이면 insert" 패턴이라 몇 번을 다시 돌려도 결과가 같다(§9).
 */
@Service
public class FinancialSyncServiceImpl implements FinancialSyncService {

    private static final List<String> SOURCE_ORDER =
            List.of("BANK", "DEPOSIT", "SECURITIES", "LOAN", "PAY_MONEY", "CARD");

    /**
     * 수집이 아니라 **저장 단계**에서 실패했다는 표시. 이력의 failed_source 에 들어간다 —
     * 소스 이름(BANK 등)과 섞이지 않게 별도 값을 쓴다. (컬럼은 VARCHAR(20))
     */
    private static final String SAVE_PHASE = "SAVE";

    /** tbl_financial_sync_history.fail_reason 은 VARCHAR(500) 이다. DB 예외 메시지는 이보다 길 수 있다. */
    private static final int FAIL_REASON_MAX = 500;

    /** rawJson 파싱 전용. ObjectMapper 는 스레드 안전하므로 한 번만 만든다. */
    private static final ObjectMapper RAW_JSON_READER = new ObjectMapper();

    private final FinancialSyncClient client;
    private final ScenarioKeyProvider scenarioKeyProvider;
    private final ConnectedAccountMapper connectedAccountMapper;
    private final LoanMapper loanMapper;
    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final CardMapper cardMapper;
    private final CardBillMapper cardBillMapper;
    private final TransactionMapper transactionMapper;
    private final FinancialSyncHistoryMapper syncHistoryMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    /**
     * ⚠ 생성자가 둘이라 **@Autowired 로 어느 쪽을 쓸지 명시해야 한다** (AccountLinkService 와 같은 이유).
     *   컨텍스트에 Clock 빈이 없으므로 Spring 이 쓰는 쪽은 이 생성자다 — Clock 을 파라미터로 남겨 두면
     *   `NoSuchBeanDefinitionException: java.time.Clock` 으로 기동이 실패한다.
     *   단위 테스트는 아래 패키지 전용 생성자로 시간을 고정한다.
     */
    @Autowired
    public FinancialSyncServiceImpl(FinancialSyncClient client,
                                    ScenarioKeyProvider scenarioKeyProvider,
                                    ConnectedAccountMapper connectedAccountMapper,
                                    LoanMapper loanMapper,
                                    InvestmentHoldingMapper investmentHoldingMapper,
                                    CardMapper cardMapper,
                                    CardBillMapper cardBillMapper,
                                    TransactionMapper transactionMapper,
                                    FinancialSyncHistoryMapper syncHistoryMapper,
                                    TransactionTemplate transactionTemplate) {
        this(client, scenarioKeyProvider, connectedAccountMapper, loanMapper, investmentHoldingMapper,
                cardMapper, cardBillMapper, transactionMapper, syncHistoryMapper,
                Clock.systemDefaultZone(), transactionTemplate);
    }

    /** 테스트에서 시간을 고정하기 위한 생성자. */
    FinancialSyncServiceImpl(FinancialSyncClient client,
                             ScenarioKeyProvider scenarioKeyProvider,
                             ConnectedAccountMapper connectedAccountMapper,
                             LoanMapper loanMapper,
                             InvestmentHoldingMapper investmentHoldingMapper,
                             CardMapper cardMapper,
                             CardBillMapper cardBillMapper,
                             TransactionMapper transactionMapper,
                             FinancialSyncHistoryMapper syncHistoryMapper,
                             Clock clock,
                             TransactionTemplate transactionTemplate) {
        this.client = client;
        this.scenarioKeyProvider = scenarioKeyProvider;
        this.connectedAccountMapper = connectedAccountMapper;
        this.loanMapper = loanMapper;
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.cardMapper = cardMapper;
        this.cardBillMapper = cardBillMapper;
        this.transactionMapper = transactionMapper;
        this.syncHistoryMapper = syncHistoryMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public FinancialSyncResultDto sync(long userId) {
        LocalDateTime startedAt = LocalDateTime.now(clock);
        String scenarioKey = scenarioKeyProvider.resolve(userId);

        /*
         * 지금 처리 중인 소스를 담아 두는 커서. 실패 이력에 소스명을 남겨야 하는데 예외 자체에는
         * 그 정보가 없다. 인스턴스 필드로 두면 싱글턴 빈이라 동시 요청끼리 서로 덮어쓰므로
         * 호출마다 새로 만든 지역 객체를 넘긴다.
         */
        SourceCursor cursor = new SourceCursor();
        SyncBundle bundle;
        try {
            bundle = collectAll(scenarioKey, cursor);
        } catch (BusinessException e) {
            recordFailure(userId, cursor.value, e.getMessage(), startedAt);
            throw e;
        }

        /*
         * 쓰기 실패도 이력을 남긴다. 트랜잭션은 이미 롤백된 뒤라(saveAll 이 경계를 닫고 나온다)
         * 이 insert 는 별도 트랜잭션으로 커밋된다 — 실패 이력이 롤백에 휩쓸리지 않는다.
         * 이게 없으면 저장 단계 실패는 이력 한 줄 없이 500 으로만 남는다.
         */
        try {
            saveAll(userId, bundle, startedAt);
        } catch (RuntimeException e) {
            recordFailure(userId, SAVE_PHASE, e.getMessage(), startedAt);
            throw e;
        }

        return FinancialSyncResultDto.builder()
                .status("COMPLETED")
                .syncedSources(SOURCE_ORDER)
                .syncedAt(LocalDateTime.now(clock).atZone(clock.getZone())
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    /* ── 수집 (트랜잭션 밖) ─────────────────────────────── */

    /** 전부 트랜잭션 밖. 하나라도 실패하면 즉시 BusinessException 이 올라간다(fail-fast). */
    private SyncBundle collectAll(String scenarioKey, SourceCursor cursor) {
        SyncBundle bundle = new SyncBundle();

        cursor.value = "BANK";
        for (BankAccountSyncDto account : client.getBankAccounts(scenarioKey)) {
            bundle.bankAccounts.add(account);
            bundle.bankTransactions.put(account.getAccountId(),
                    client.getBankTransactions(scenarioKey, account.getAccountId()));
        }

        cursor.value = "DEPOSIT";
        for (DepositSyncDto deposit : client.getDeposits(scenarioKey)) {
            bundle.deposits.add(deposit);
            bundle.depositTransactions.put(deposit.getDepositAccountId(),
                    client.getDepositTransactions(scenarioKey, deposit.getDepositAccountId()));
        }

        cursor.value = "SECURITIES";
        StockAssetSyncDto stock = client.getStockAsset(scenarioKey);
        if (stock != null) {
            bundle.stock = stock;
            bundle.securitiesTransactions.put(stock.getAccountId(),
                    client.getSecuritiesTransactions(scenarioKey, stock.getAccountId()));
        }

        cursor.value = "LOAN";
        for (LoanSyncDto loan : client.getLoans(scenarioKey)) {
            bundle.loans.add(loan);
            bundle.loanTransactions.put(loan.getLoanId(),
                    client.getLoanTransactions(scenarioKey, loan.getLoanId()));
        }

        cursor.value = "PAY_MONEY";
        for (PayMoneySyncDto payMoney : client.getPayMoney(scenarioKey)) {
            bundle.payMoney.add(payMoney);
            bundle.payMoneyTransactions.put(payMoney.getPayMoneyId(),
                    client.getPayMoneyTransactions(scenarioKey, payMoney.getPayMoneyId()));
        }

        cursor.value = "CARD";
        for (CardSyncDto card : client.getCards(scenarioKey)) {
            bundle.cards.add(card);
            bundle.cardApprovals.put(card.getCardId(), client.getCardApprovals(scenarioKey, card.getCardId()));
            bundle.cardBills.put(card.getCardId(), client.getCardBills(scenarioKey, card.getCardId()));
        }

        return bundle;
    }

    private void recordFailure(long userId, String failedSource, String failReason,
                               LocalDateTime startedAt) {
        syncHistoryMapper.insert(FinancialSyncHistory.builder()
                .userId(userId)
                .status("FAILED")
                .failedSource(failedSource)
                .failReason(truncate(failReason))
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now(clock))
                .build());
    }

    /* ── 저장 (하나의 트랜잭션) ─────────────────────────── */

    /**
     * 저장 구간의 트랜잭션 경계.
     *
     * ⚠ 여기서 @Transactional 을 쓰면 **아무 일도 일어나지 않는다.** 애너테이션은 프록시를 통해 들어온
     *    호출에만 걸리는데 이 메서드는 같은 객체의 sync() 가 직접 부르기 때문이다(자기호출).
     *    설계 §8 이 요구하는 "전부 성공한 뒤 하나의 커밋" 을 실제로 보장하려면 경계를 코드로 명시해야
     *    해서 TransactionTemplate 을 쓴다. 콜백 안에서 예외가 나면 그때까지의 쓰기가 전부 롤백된다.
     */
    private void saveAll(long userId, SyncBundle bundle, LocalDateTime startedAt) {
        transactionTemplate.execute(status -> {
            writeAll(userId, bundle, startedAt);
            return null;
        });
    }

    /** 실제 쓰기. 순서: 상품 upsert → 거래 upsert → correlation 연결 → 성공 이력. */
    private void writeAll(long userId, SyncBundle bundle, LocalDateTime startedAt) {
        LocalDateTime now = LocalDateTime.now(clock);

        /* BANK: 계좌 upsert + 거래 upsert. 계좌 타입은 목서버 accountTypeCode 를 그대로 안 쓰고
           우리 도메인 값(DEMAND_DEPOSIT)으로 저장한다 — AccountLinkService 관례와 동일. */
        for (BankAccountSyncDto account : bundle.bankAccounts) {
            Long connectedAccountId = upsertBankAccount(userId, account, now);
            for (BankTransactionSyncDto tx :
                    bundle.bankTransactions.getOrDefault(account.getAccountId(), List.of())) {
                /*
                 * 은행 출금 쪽에도 correlationId 를 채운다. linkByCorrelation() 이 BANK ↔ CARD_CHECK 를
                 * correlation_id 로 잇는데(설계 §7), 여기서 비워 두면 연결이 한 건도 성립하지 않는다.
                 */
                RawJsonFields fields = RawJsonFields.parse(tx.getRawJson());
                saveTransaction(userId, connectedAccountId, null, "BANK",
                        "BANK-" + connectedAccountId + "-" + tx.getTransactionId(),
                        tx.getAmount(), tx.getTransactedAt(), tx.getDescription(),
                        null, null, fields.correlationId, null, tx.getRawJson());
            }
        }

        /* CARD: 카드 upsert -> 승인건은 거래로, 청구서는 별도 테이블로. */
        for (CardSyncDto card : bundle.cards) {
            Long cardId = upsertCard(userId, card, now);
            /* 01=신용, 02=체크. 목서버에 공식 enum 이 없어 시드 관례를 따른다(설계 「미해결」 항목). */
            String sourceType = "01".equals(card.getCardTypeCode()) ? "CARD_CREDIT" : "CARD_CHECK";
            for (CardApprovalSyncDto approval :
                    bundle.cardApprovals.getOrDefault(card.getCardId(), List.of())) {
                RawJsonFields fields = RawJsonFields.parse(approval.getRawJson());
                /* 원거래 승인번호는 취소·환불 후속 처리가 참조한다(설계 §7). 컬럼에도 남긴다. */
                saveTransaction(userId, null, cardId, sourceType,
                        sourceType + "-" + cardId + "-" + approval.getApprovalNo(),
                        approval.getApprovedAmount(), approval.getApprovedAt(), approval.getMerchantName(),
                        approval.getMerchantCategoryCode(), approval.getMerchantCategoryName(),
                        fields.correlationId, fields.originalApprovalNo, approval.getRawJson());
            }
            for (CardBillSyncDto bill : bundle.cardBills.getOrDefault(card.getCardId(), List.of())) {
                upsertCardBill(cardId, bill);
            }
        }

        /* 나머지 소스(DEPOSIT/SECURITIES/LOAN/PAY_MONEY)도 같은 upsert-then-transaction 패턴. */
        saveDeposits(userId, bundle, now);
        saveSecurities(userId, bundle, now);
        saveLoans(userId, bundle);
        savePayMoney(userId, bundle, now);

        /* 반환값은 연결된 쌍의 수가 아니라 갱신된 행 수(쌍당 2)다 — 다중 테이블 UPDATE 라서. */
        transactionMapper.linkByCorrelation(userId);

        syncHistoryMapper.insert(FinancialSyncHistory.builder()
                .userId(userId)
                .status("COMPLETED")
                .syncedSourcesJson(toJsonArray(SOURCE_ORDER))
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now(clock))
                .build());
    }

    private void saveDeposits(long userId, SyncBundle bundle, LocalDateTime now) {
        for (DepositSyncDto deposit : bundle.deposits) {
            String accountNoEncrypted = "MOCK-DEPOSIT-" + deposit.getDepositAccountId();
            ConnectedAccount row = ConnectedAccount.builder()
                    .userId(userId)
                    .bankCode(deposit.getInstitutionCode())
                    .bankName(deposit.getInstitutionName())
                    .accountName(deposit.getProductName())
                    .accountNoEncrypted(accountNoEncrypted)
                    .accountNoMasked(deposit.getAccountNoMasked())
                    .accountType("SAVINGS")
                    .balance(deposit.getBalance())
                    .syncStatus("NORMAL")
                    .lastSyncAt(now)
                    .build();
            Long accountId = upsertConnectedAccount(userId, row, accountNoEncrypted);
            for (DepositTransactionSyncDto tx :
                    bundle.depositTransactions.getOrDefault(deposit.getDepositAccountId(), List.of())) {
                saveTransaction(userId, accountId, null, "DEPOSIT",
                        "DEPOSIT-" + accountId + "-" + tx.getTransactionId(),
                        tx.getAmount(), tx.getTransactedAt(), tx.getDescription(),
                        null, null, null, null, tx.getRawJson());
            }
        }
    }

    private void saveSecurities(long userId, SyncBundle bundle, LocalDateTime now) {
        if (bundle.stock == null) {
            return;
        }
        String accountNoEncrypted = "MOCK-SECURITIES-" + bundle.stock.getAccountId();
        ConnectedAccount row = ConnectedAccount.builder()
                .userId(userId)
                .bankName(bundle.stock.getInstitutionName())
                .accountNoEncrypted(accountNoEncrypted)
                .accountType("SECURITIES")
                /* 현금잔액·평가금액은 상품 조회값을 쓴다. 거래 합산으로 재계산하지 않는다(설계 §7). */
                .balance(bundle.stock.getCashBalance())
                .syncStatus("NORMAL")
                .lastSyncAt(now)
                .build();
        Long accountId = upsertConnectedAccount(userId, row, accountNoEncrypted);

        for (StockHoldingSyncDto holding : holdings(bundle.stock)) {
            InvestmentHolding ih = InvestmentHolding.builder()
                    .userId(userId)
                    .accountId(accountId)
                    .symbol(holding.getProductCode())
                    .name(holding.getProductName())
                    /* 목서버에 국가 정보가 없다. 시드된 종목이 전부 국내라 고정값을 쓴다(도메인 주석 참고). */
                    .marketCountry("KR")
                    .currency(bundle.stock.getCurrency())
                    .quantity(holding.getQuantity())
                    .averagePurchasePrice(holding.getAveragePurchasePrice())
                    .lastPrice(holding.getLastPrice())
                    .purchaseAmount(holding.getPurchaseAmount())
                    .marketValue(holding.getMarketValue())
                    .profitLossAmount(holding.getProfitLossAmount())
                    .profitLossRate(holding.getProfitLossRate())
                    .build();
            if (investmentHoldingMapper.update(ih) == 0) {
                investmentHoldingMapper.insert(ih);
            }
        }
        for (SecuritiesTransactionSyncDto tx :
                bundle.securitiesTransactions.getOrDefault(bundle.stock.getAccountId(), List.of())) {
            saveTransaction(userId, accountId, null, "SECURITIES",
                    "SECURITIES-" + accountId + "-" + tx.getTransactionId(),
                    tx.getTransactionAmount(), tx.getTransactedAt(),
                    tx.getSecurityProductName(), null, null, null, null, null);
        }
    }

    private void saveLoans(long userId, SyncBundle bundle) {
        for (LoanSyncDto loan : bundle.loans) {
            Loan row = Loan.builder()
                    .userId(userId)
                    /* 목서버는 마스킹된 대출번호만 준다 — 해시할 원본이 없어 소스 식별자를 그대로 쓴다. */
                    .loanNoEncrypted("MOCK-LOAN-" + loan.getLoanId())
                    .bankName(loan.getInstitutionName())
                    .loanType(loan.getProductName())
                    .loanAmount(loan.getPrincipal())
                    .balance(loan.getBalance())
                    .interestRate(loan.getInterestRate())
                    .startDate(date(loan.getStartDate()))
                    .maturityDate(date(loan.getMaturityDate()))
                    .monthlyPayment(loan.getMonthlyPayment())
                    .nextPaymentDate(date(loan.getNextPaymentDate()))
                    .build();
            Long loanId;
            if (loanMapper.update(row) > 0) {
                /* 방금 갱신한 행을 못 찾는 건 이상 상황이다. null 로 두면 account_id·loan_id 가 둘 다
                   빈 고아 거래가 생기므로, 다른 upsert 헬퍼들과 똑같이 예외로 끊는다. */
                loanId = loanMapper.findByUser(userId).stream()
                        .filter(l -> row.getLoanNoEncrypted().equals(l.getLoanNoEncrypted()))
                        .findFirst()
                        .map(Loan::getId)
                        .orElseThrow(() -> new BusinessException("EXTERNAL_API_ERROR",
                                "동기화한 대출을 다시 찾지 못했어요."));
            } else {
                loanMapper.insert(row);
                loanId = row.getId();   // useGeneratedKeys 로 insert 직후 채워진다
            }

            for (LoanTransactionSyncDto tx :
                    bundle.loanTransactions.getOrDefault(loan.getLoanId(), List.of())) {
                Transaction txRow = Transaction.builder()
                        .userId(userId)
                        .loanId(loanId)
                        .codefTrKey("LOAN-" + loanId + "-" + tx.getTransactionId())
                        .amount(tx.getAmount())
                        /* 대출 거래는 실행(입금)과 상환(출금)이 섞여 있다 — 구분이 서기 전엔 비워 둔다. */
                        .direction(null)
                        .trDate(OffsetDateTime.parse(tx.getTransactedAt()).toLocalDate())
                        .classification("TRANSFER")
                        .isExcludedFromSummary(false)
                        .sourceType("LOAN")
                        .rawJson(tx.getRawJson())
                        .build();
                if (transactionMapper.update(txRow) == 0) {
                    transactionMapper.insert(txRow);
                }
            }
        }
    }

    private void savePayMoney(long userId, SyncBundle bundle, LocalDateTime now) {
        for (PayMoneySyncDto payMoney : bundle.payMoney) {
            String accountNoEncrypted = "MOCK-PAYMONEY-" + payMoney.getPayMoneyId();
            ConnectedAccount row = ConnectedAccount.builder()
                    .userId(userId)
                    .bankName(payMoney.getProviderName())
                    .accountName(payMoney.getWalletName())
                    .accountNoEncrypted(accountNoEncrypted)
                    .accountType("PAYMONEY")
                    .provider(payMoney.getProviderCode())
                    .balance(payMoney.getBalance())
                    .syncStatus("NORMAL")
                    .lastSyncAt(now)
                    .build();
            Long accountId = upsertConnectedAccount(userId, row, accountNoEncrypted);
            for (PayMoneyTransactionSyncDto tx :
                    bundle.payMoneyTransactions.getOrDefault(payMoney.getPayMoneyId(), List.of())) {
                saveTransaction(userId, accountId, null, "PAYMONEY",
                        "PAYMONEY-" + accountId + "-" + tx.getTransactionId(),
                        tx.getAmount(), tx.getTransactedAt(), tx.getMerchantName(),
                        tx.getMerchantCategoryCode(), tx.getMerchantCategoryName(),
                        null, null, tx.getRawJson());
            }
        }
    }

    /* ── upsert 헬퍼 ───────────────────────────────────── */

    private Long upsertBankAccount(long userId, BankAccountSyncDto account, LocalDateTime now) {
        String accountType = "SAVINGS".equalsIgnoreCase(account.getAccountTypeCode())
                ? "SAVINGS" : "DEMAND_DEPOSIT";
        String accountNoEncrypted = "MOCK-BANK-" + account.getAccountId();
        ConnectedAccount row = ConnectedAccount.builder()
                .userId(userId)
                .bankCode(account.getInstitutionCode())
                .bankName(account.getInstitutionName())
                .accountName(account.getProductName())
                .accountNoEncrypted(accountNoEncrypted)
                .accountNoMasked(account.getAccountNoMasked())
                .accountType(accountType)
                .balance(account.getBalance())
                .syncStatus("NORMAL")
                .lastSyncAt(now)
                .build();
        return upsertConnectedAccount(userId, row, accountNoEncrypted);
    }

    /**
     * BANK/DEPOSIT/SECURITIES/PAYMONEY 공통 upsert (reactivate 시도 → 0행이면 insert). PK 를 돌려준다.
     *
     * ⚠ 계좌번호 원본이 없는 목서버 데이터라 account_no_encrypted 에 "MOCK-{소스}-{목서버ID}" 를 넣는다.
     *   HMAC 해시가 아니므로 AccountLinkService 로 연결한 실제 계좌 행과 겹치지 않는다.
     */
    private Long upsertConnectedAccount(long userId, ConnectedAccount row, String accountNoEncrypted) {
        if (connectedAccountMapper.reactivate(row) > 0) {
            /* update 경로는 PK 를 돌려주지 않는다. 자연키로 다시 찾는다. */
            return connectedAccountMapper.findActiveByUser(userId).stream()
                    .filter(a -> accountNoEncrypted.equals(a.getAccountNoEncrypted()))
                    .findFirst()
                    .map(ConnectedAccount::getId)
                    .orElseThrow(() -> new BusinessException("EXTERNAL_API_ERROR",
                            "동기화한 계좌를 다시 찾지 못했어요."));
        }
        connectedAccountMapper.insert(row);
        return row.getId();     // useGeneratedKeys 로 insert 직후 채워진다
    }

    private Long upsertCard(long userId, CardSyncDto card, LocalDateTime now) {
        Card row = Card.builder()
                .userId(userId)
                .institutionCode(card.getInstitutionCode())
                .institutionName(card.getInstitutionName())
                .cardNoMasked(card.getCardNoMasked())
                .productName(card.getProductName())
                .cardProductCode(card.getCardProductCode())
                .cardTypeCode(card.getCardTypeCode())
                .cardStatusCode(card.getCardStatusCode())
                .currency(card.getCurrency())
                .issuedAt(date(card.getIssuedAt()))
                .lastSyncAt(now)
                .build();
        if (cardMapper.update(row) > 0) {
            return cardMapper.findByUser(userId).stream()
                    .filter(c -> card.getCardNoMasked().equals(c.getCardNoMasked()))
                    .findFirst()
                    .map(Card::getId)
                    .orElseThrow(() -> new BusinessException("EXTERNAL_API_ERROR",
                            "동기화한 카드를 다시 찾지 못했어요."));
        }
        cardMapper.insert(row);
        return row.getId();
    }

    /** 카드 청구서는 거래가 아니다 — tbl_card_bill 에만 넣는다(설계 §7). */
    private void upsertCardBill(Long cardId, CardBillSyncDto bill) {
        CardBill row = CardBill.builder()
                .cardId(cardId)
                .billingMonth(bill.getBillingMonth())
                .dueDate(date(bill.getDueDate()))
                .billStatusCode(bill.getBillStatusCode())
                .billStatusName(bill.getBillStatusName())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .build();
        if (cardBillMapper.update(row) == 0) {
            cardBillMapper.insert(row);
        }
    }

    /**
     * 거래 한 건 upsert. 멱등키(codef_tr_key) 로 update 를 먼저 시도하고 0행이면 insert 한다 —
     * 재동기화로 같은 거래가 다시 들어와도 행이 늘지 않는다(설계 §9).
     *
     * ⚠ codefTrKey 의 상품 ID 자리에는 **반드시 우리 DB 의 PK** 를 넣는다(목서버 ID 금지).
     *   `uk_tx_codef_key` 는 codef_tr_key 단독 UNIQUE 이고 update 의 WHERE 에도 user_id 가 없다.
     *   목서버 ID 를 쓰면 시나리오 키를 공유하는 사용자들이 같은 키를 만들어, 두 번째 사용자의 동기화가
     *   **첫 번째 사용자의 거래를 덮어쓰고 자기 행은 만들지 못한다**(6명이 같은 목서버로 데모한다).
     *   우리 PK 는 상품 자연키 upsert 로 사용자마다 다르고 재동기화 사이에는 안 변한다.
     */
    private void saveTransaction(long userId, Long accountId, Long cardId, String sourceType,
                                 String codefTrKey, BigDecimal amount, String transactedAt,
                                 String merchantName, String merchantCategoryCode,
                                 String merchantCategoryName, String correlationId,
                                 String originalApprovalNo, String rawJson) {
        Transaction row = Transaction.builder()
                .userId(userId)
                .accountId(accountId)
                .cardId(cardId)
                .codefTrKey(codefTrKey)
                .merchantName(merchantName)
                .merchantNameNormalized(merchantName)
                .amount(amount)
                /*
                 * 금액 부호로는 입출금을 못 가른다 — tbl_transaction.amount 는 "항상 양수" 이고
                 * 목서버도 양수만 준다. 카드 승인은 성격상 무조건 출금이라 OUT 으로 확정하고,
                 * 나머지 소스는 원천 거래유형 코드의 의미가 확정되지 않아 **지어내지 않고 비운다**
                 * (direction 은 nullable). 코드 해석이 정리되면 그때 채운다.
                 */
                .direction(sourceType.startsWith("CARD_") ? "OUT" : null)
                .trDate(OffsetDateTime.parse(transactedAt).toLocalDate())
                .classification("CONSUMPTION")
                .isExcludedFromSummary(false)
                .sourceType(sourceType)
                .correlationId(correlationId)
                .originalApprovalNo(originalApprovalNo)
                .merchantCategoryCode(merchantCategoryCode)
                .merchantCategoryName(merchantCategoryName)
                .rawJson(rawJson)
                .build();
        if (transactionMapper.update(row) == 0) {
            transactionMapper.insert(row);
        }
    }

    /* ── 잡동사니 ──────────────────────────────────────── */

    private static List<StockHoldingSyncDto> holdings(StockAssetSyncDto stock) {
        return stock.getHoldings() == null ? List.of() : stock.getHoldings();
    }

    /** 실패 사유를 컬럼 길이에 맞춘다 — 이력 남기려다 이력 insert 가 터지면 본말전도다. */
    private static String truncate(String reason) {
        if (reason == null || reason.length() <= FAIL_REASON_MAX) {
            return reason;
        }
        return reason.substring(0, FAIL_REASON_MAX);
    }

    /** 목서버 날짜는 전부 yyyy-MM-dd 다. 빈 값은 그대로 비워 둔다. */
    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append('"').append(values.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    /** collectAll 이 지금 어느 소스를 처리 중인지. 호출마다 새로 만들어 넘긴다(싱글턴 빈이라 필드 금지). */
    private static final class SourceCursor {
        private String value;
    }

    /** 목서버 rawJson 문자열에서 correlationId/originalApprovalNo 만 뽑는다. 나머지 키는 무시한다. */
    private static final class RawJsonFields {
        private final String correlationId;
        private final String originalApprovalNo;

        private RawJsonFields(String correlationId, String originalApprovalNo) {
            this.correlationId = correlationId;
            this.originalApprovalNo = originalApprovalNo;
        }

        static RawJsonFields parse(String rawJson) {
            if (rawJson == null || rawJson.isBlank()) {
                return new RawJsonFields(null, null);
            }
            try {
                JsonNode node = RAW_JSON_READER.readTree(rawJson);
                return new RawJsonFields(
                        node.hasNonNull("correlationId") ? node.get("correlationId").asText() : null,
                        node.hasNonNull("originalApprovalNo")
                                ? node.get("originalApprovalNo").asText() : null);
            } catch (Exception e) {
                /* raw_json 파싱 실패는 동기화 전체를 막을 이유가 아니다 — 연결 정보 없이 저장한다. */
                return new RawJsonFields(null, null);
            }
        }
    }

    /**
     * 수집 결과 묶음. 거래는 **상품별 Map** 으로 담는다 — 하나의 평면 리스트에 모으면 계좌가 둘 이상일 때
     * 어느 계좌의 거래인지 잃어버려 서로 섞인다.
     */
    private static final class SyncBundle {
        final List<BankAccountSyncDto> bankAccounts = new ArrayList<>();
        final Map<Long, List<BankTransactionSyncDto>> bankTransactions = new HashMap<>();
        final List<DepositSyncDto> deposits = new ArrayList<>();
        final Map<Long, List<DepositTransactionSyncDto>> depositTransactions = new HashMap<>();
        StockAssetSyncDto stock;
        final Map<Long, List<SecuritiesTransactionSyncDto>> securitiesTransactions = new HashMap<>();
        final List<LoanSyncDto> loans = new ArrayList<>();
        final Map<Long, List<LoanTransactionSyncDto>> loanTransactions = new HashMap<>();
        final List<PayMoneySyncDto> payMoney = new ArrayList<>();
        final Map<Long, List<PayMoneyTransactionSyncDto>> payMoneyTransactions = new HashMap<>();
        final List<CardSyncDto> cards = new ArrayList<>();
        final Map<Long, List<CardApprovalSyncDto>> cardApprovals = new HashMap<>();
        final Map<Long, List<CardBillSyncDto>> cardBills = new HashMap<>();
    }
}

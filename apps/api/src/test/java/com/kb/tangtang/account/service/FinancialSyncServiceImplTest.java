package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.sync.FinancialSyncClient;
import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
import com.kb.tangtang.account.client.sync.dto.BankAccountSyncDto;
import com.kb.tangtang.account.client.sync.dto.BankTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardApprovalSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardSyncDto;
import com.kb.tangtang.account.client.sync.dto.DepositSyncDto;
import com.kb.tangtang.account.client.sync.dto.DepositTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.PayMoneySyncDto;
import com.kb.tangtang.account.client.sync.dto.PayMoneyTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.SecuritiesTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.StockAssetSyncDto;
import com.kb.tangtang.account.domain.Card;
import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.account.domain.LlmCategorizationRequestedEvent;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.mapper.CardBillMapper;
import com.kb.tangtang.account.mapper.CardMapper;
import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import com.kb.tangtang.account.mapper.FinancialSyncHistoryMapper;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.dto.RuleCategorizationResultDto;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import com.kb.tangtang.transaction.service.TransactionCategorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이슈 #147 — 금융 동기화 오케스트레이터 단위 테스트.
 * 매퍼·클라이언트는 전부 목이라 DB·목서버가 없어도 돈다 (AccountLinkServiceTest 와 같은 방식).
 */
class FinancialSyncServiceImplTest {

    /**
     * 트랜잭션 개시·커밋과 DB 쓰기를 **한 줄에** 기록한다. 순서를 검증하려면 둘을 같은 타임라인에
     * 올려야 한다 — 트랜잭션 쪽은 목이 아니라 실제 구현이라 Mockito InOrder 하나로는 못 묶는다.
     */
    private List<String> timeline;

    /**
     * 커넥션 없이 콜백만 그대로 실행하는 트랜잭션 매니저.
     *
     * TransactionTemplate 은 매니저를 목으로 줘도 콜백을 부르긴 하지만, 그건 목의 기본 반환값
     * (getTransaction → null)에 기대는 동작이라 의도가 드러나지 않는다. 여기서는 개시·커밋 시점을
     * 직접 기록해 경계 순서까지 검증한다. 실제 경계는 RootConfig 의 빈이 담당한다.
     */
    private PlatformTransactionManager recordingTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                timeline.add("BEGIN");
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                timeline.add("COMMIT");
            }

            @Override
            public void rollback(TransactionStatus status) {
                timeline.add("ROLLBACK");
            }
        };
    }

    private FinancialSyncClient client;
    private ScenarioKeyProvider scenarioKeyProvider;
    private ConnectedAccountMapper connectedAccountMapper;
    private LoanMapper loanMapper;
    private InvestmentHoldingMapper investmentHoldingMapper;
    private CardMapper cardMapper;
    private CardBillMapper cardBillMapper;
    private TransactionMapper transactionMapper;
    private FinancialSyncHistoryMapper syncHistoryMapper;
    private TransactionCategorizationService transactionCategorizationService;
    private ApplicationEventPublisher eventPublisher;
    private FinancialSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        client = mock(FinancialSyncClient.class);
        scenarioKeyProvider = mock(ScenarioKeyProvider.class);
        connectedAccountMapper = mock(ConnectedAccountMapper.class);
        loanMapper = mock(LoanMapper.class);
        investmentHoldingMapper = mock(InvestmentHoldingMapper.class);
        cardMapper = mock(CardMapper.class);
        cardBillMapper = mock(CardBillMapper.class);
        transactionMapper = mock(TransactionMapper.class);
        syncHistoryMapper = mock(FinancialSyncHistoryMapper.class);
        timeline = new ArrayList<>();

        Clock clock = Clock.fixed(Instant.parse("2026-08-12T01:00:00Z"), ZoneId.of("Asia/Seoul"));

        transactionCategorizationService = mock(TransactionCategorizationService.class);
        when(transactionCategorizationService.categorizeRuleBased(anyLong(), any()))
                .thenReturn(RuleCategorizationResultDto.builder()
                        .ruleCategorizedCount(0)
                        .llmEligibleTransactionIds(List.of())
                        .build());
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new FinancialSyncServiceImpl(
                client, scenarioKeyProvider, connectedAccountMapper, loanMapper,
                investmentHoldingMapper, cardMapper, cardBillMapper, transactionMapper,
                syncHistoryMapper, transactionCategorizationService, eventPublisher,
                clock, new TransactionTemplate(recordingTransactionManager()));

        when(scenarioKeyProvider.resolve(1L)).thenReturn("1");
        // 이번 테스트는 BANK 만 데이터가 있고 나머지 5개 소스는 빈 목록으로 둔다.
        when(client.getBankAccounts("1")).thenReturn(List.of(
                BankAccountSyncDto.builder()
                        .accountId(101L).institutionCode("0004").institutionName("KB국민은행")
                        .accountTypeCode("1001").productName("KB Star Checking")
                        .accountNoMasked("110-***-120045").currency("KRW")
                        .balance(new BigDecimal("1244200")).availableAmount(new BigDecimal("1244200"))
                        .build()));
        when(client.getBankTransactions(eq("1"), eq(101L))).thenReturn(List.of(
                BankTransactionSyncDto.builder()
                        .transactionId(9001L).transactedAt("2026-08-10T09:05:01+09:00")
                        .transTypeCode("01").amount(new BigDecimal("50000"))
                        .balanceAfter(new BigDecimal("1194200")).description("이체")
                        .rawJson(null)
                        .build()));
        when(client.getDeposits("1")).thenReturn(List.of());
        when(client.getStockAsset("1")).thenReturn(null);
        when(client.getLoans("1")).thenReturn(List.of());
        when(client.getPayMoney("1")).thenReturn(List.of());
        when(client.getCards("1")).thenReturn(List.of());

        /* reactivate 가 0 을 돌려주므로 기본 시나리오는 insert 경로다. update 경로는
           resyncReusesExistingRowOnUpdatePath 가 따로 검증한다. */
        when(connectedAccountMapper.insert(any())).thenAnswer(inv -> 1);
        when(transactionMapper.update(any())).thenReturn(0);
        when(transactionMapper.insert(any())).thenReturn(1);
    }

    @Test
    @DisplayName("모든 소스가 성공하면 COMPLETED 를 반환하고 이력을 성공으로 남긴다")
    void syncSucceeds() {
        FinancialSyncResultDto result = service.sync(1L);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(List.of("BANK", "DEPOSIT", "SECURITIES", "LOAN", "PAY_MONEY", "CARD"),
                result.getSyncedSources());
        verify(syncHistoryMapper).insert(argThat(h -> "COMPLETED".equals(h.getStatus())));
        verify(transactionMapper).linkByCorrelation(1L);
    }

    @Test
    @DisplayName("한 소스라도 실패하면 DB 를 건드리지 않고 실패 이력만 남긴다")
    void oneSourceFailsAbortsWholeSync() {
        when(client.getCards("1")).thenThrow(
                new BusinessException("EXTERNAL_API_UNAVAILABLE", "목서버 응답 없음"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.sync(1L));

        assertEquals("EXTERNAL_API_UNAVAILABLE", e.getCode());
        verify(connectedAccountMapper, never()).insert(any());
        verify(transactionMapper, never()).insert(any());
        verify(syncHistoryMapper).insert(argThat(h ->
                "FAILED".equals(h.getStatus()) && "CARD".equals(h.getFailedSource())));
    }

    @Test
    @DisplayName("같은 데이터로 두 번 동기화해도 거래를 두 번 insert 하지 않는다")
    void resyncIsIdempotent() {
        when(transactionMapper.update(any())).thenReturn(0).thenReturn(1); // 1차 insert, 2차 update
        when(transactionMapper.insert(any())).thenReturn(1);

        service.sync(1L);
        service.sync(1L);

        verify(transactionMapper, times(1)).insert(any());
        verify(transactionMapper, times(2)).update(any());
    }

    @Test
    @DisplayName("두 번째 동기화는 update 경로를 타고 첫 동기화가 만든 행의 PK 를 그대로 재사용한다")
    void resyncReusesExistingRowOnUpdatePath() {
        /*
         * 1차: reactivate 0행 → insert (PK 77 부여). 2차: reactivate 1행 → 자연키 재조회로 같은 77.
         * 이 스텁이 없으면 두 번째 sync 도 insert 분기를 타 update 경로(재조회 포함)가 통째로
         * 검증되지 않는다 — 예전 테스트가 그랬다.
         */
        when(connectedAccountMapper.insert(any())).thenAnswer(inv -> {
            ConnectedAccount saved = inv.getArgument(0);
            saved.setId(77L);   // useGeneratedKeys 흉내
            return 1;
        });
        when(connectedAccountMapper.reactivate(any())).thenReturn(0).thenReturn(1);
        when(connectedAccountMapper.findActiveByUser(1L)).thenReturn(List.of(
                ConnectedAccount.builder().id(77L).userId(1L)
                        .accountNoEncrypted("MOCK-BANK-101").build()));
        when(transactionMapper.update(any())).thenReturn(0).thenReturn(1);

        service.sync(1L);
        service.sync(1L);

        /* 계좌는 한 번만 만들어진다 — 2차는 되살리기(update) 경로다. */
        verify(connectedAccountMapper, times(1)).insert(any());
        verify(connectedAccountMapper, times(2)).reactivate(any());
        /* 핵심: 2차의 멱등키가 1차와 같은 내부 PK(77)를 쓴다. 목서버 ID(101)를 쓰거나 재조회가
           엉뚱한 행을 집으면 새 행이 하나 더 생긴다. */
        verify(transactionMapper, times(2)).update(argThat(t -> "BANK-77-9001".equals(t.getCodefTrKey())));
        verify(transactionMapper, times(1)).insert(argThat(t -> "BANK-77-9001".equals(t.getCodefTrKey())));
    }

    @Test
    @DisplayName("card_type_code 01=CARD_CREDIT, 02=CARD_CHECK 으로 분류하고, 동기화 후 연결 SQL 을 부른다")
    void classifiesCardTypesAndLinksCorrelation() {
        when(client.getCards("1")).thenReturn(List.of(
                CardSyncDto.builder().cardId(1L).cardNoMasked("9490-****-****-2201")
                        .cardTypeCode("01").currency("KRW").build(),
                CardSyncDto.builder().cardId(2L).cardNoMasked("5210-****-****-7714")
                        .cardTypeCode("02").currency("KRW").build()));
        when(client.getCardApprovals(eq("1"), eq(1L))).thenReturn(List.of(
                CardApprovalSyncDto.builder().approvalId(1L).approvalNo("APV-CREDIT-1")
                        .approvedAt("2026-08-10T12:00:00+09:00").approvedAmount(new BigDecimal("30000"))
                        .rawJson(null).build()));
        when(client.getCardApprovals(eq("1"), eq(2L))).thenReturn(List.of(
                CardApprovalSyncDto.builder().approvalId(2L).approvalNo("APV-CHECK-1")
                        .approvedAt("2026-08-10T13:00:00+09:00").approvedAmount(new BigDecimal("15000"))
                        .rawJson("{\"correlationId\":\"N2-CHK-0617\"}").build()));
        when(client.getCardBills(anyString(), anyLong())).thenReturn(List.of());
        when(cardMapper.update(any())).thenReturn(0);
        doAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(c.getCardNoMasked().equals("9490-****-****-2201") ? 100L : 200L);
            return 1;
        }).when(cardMapper).insert(any());

        service.sync(1L);

        verify(transactionMapper, atLeastOnce()).insert(argThat(t -> "CARD_CREDIT".equals(t.getSourceType())));
        verify(transactionMapper, atLeastOnce()).insert(argThat(t ->
                "CARD_CHECK".equals(t.getSourceType()) && "N2-CHK-0617".equals(t.getCorrelationId())));
        verify(transactionMapper).linkByCorrelation(1L);
    }

    @Test
    @DisplayName("은행 거래의 raw_json 에 있는 correlationId 를 채운다 — 체크카드 연결의 전제다")
    void bankTransactionCarriesCorrelationId() {
        when(client.getBankTransactions(eq("1"), eq(101L))).thenReturn(List.of(
                BankTransactionSyncDto.builder()
                        .transactionId(9002L).transactedAt("2026-08-10T13:00:02+09:00")
                        .transTypeCode("02").amount(new BigDecimal("15000"))
                        .balanceAfter(new BigDecimal("1179200")).description("체크카드 출금")
                        .rawJson("{\"correlationId\":\"N2-CHK-0617\",\"challengeEligible\":true}")
                        .build()));

        service.sync(1L);

        verify(transactionMapper).insert(argThat(t ->
                "BANK".equals(t.getSourceType()) && "N2-CHK-0617".equals(t.getCorrelationId())));
    }

    @Test
    @DisplayName("멱등키에 목서버 ID 가 아니라 우리 DB PK 를 쓴다 — 사용자끼리 키가 충돌하면 안 된다")
    void codefTrKeyIsScopedByInternalId() {
        when(connectedAccountMapper.insert(any())).thenAnswer(inv -> {
            ConnectedAccount saved = inv.getArgument(0);
            saved.setId(77L);   // useGeneratedKeys 흉내
            return 1;
        });

        service.sync(1L);

        /* 목서버 계좌 ID(101)가 아니라 우리 PK(77)가 들어가야 한다. 101 이면 시나리오 키를 공유하는
           다른 사용자와 같은 키가 만들어져 서로의 거래를 덮어쓴다. */
        verify(transactionMapper).insert(argThat(t -> "BANK-77-9001".equals(t.getCodefTrKey())));
    }

    @Test
    @DisplayName("모든 DB 쓰기가 트랜잭션 개시 후 · 커밋 전에 일어난다 (이력 insert·연결 SQL 포함)")
    void allWritesHappenInsideOneTransaction() {
        when(connectedAccountMapper.insert(any())).thenAnswer(inv -> {
            timeline.add("WRITE:account");
            return 1;
        });
        when(transactionMapper.insert(any())).thenAnswer(inv -> {
            timeline.add("WRITE:transaction");
            return 1;
        });
        when(transactionMapper.linkByCorrelation(1L)).thenAnswer(inv -> {
            timeline.add("WRITE:link");
            return 0;
        });
        when(syncHistoryMapper.insert(any())).thenAnswer(inv -> {
            timeline.add("WRITE:history");
            return 1;
        });

        service.sync(1L);

        /*
         * 순서를 통째로 못박는다. 예전 @Transactional 자기호출 버전이었다면 BEGIN/COMMIT 자체가 없어
         * 이 단언이 깨진다. 연결 SQL 이나 이력 insert 를 나중에 트랜잭션 밖으로 빼도 마찬가지다.
         */
        assertEquals(
                List.of("BEGIN", "WRITE:account", "WRITE:transaction", "WRITE:link", "WRITE:history",
                        "COMMIT"),
                timeline);
    }

    @Test
    @DisplayName("저장 단계에서 터져도 실패 이력을 남긴다 — 500 만 뜨고 흔적이 없으면 안 된다")
    void writeFailureIsRecordedInHistory() {
        when(transactionMapper.insert(any())).thenThrow(
                new IllegalStateException("Duplicate entry 'BANK-1-9001' for key 'uk_tx_codef_key'"));

        assertThrows(IllegalStateException.class, () -> service.sync(1L));

        verify(syncHistoryMapper).insert(argThat(h ->
                "FAILED".equals(h.getStatus())
                        && "SAVE".equals(h.getFailedSource())
                        && h.getFailReason() != null && h.getFailReason().contains("Duplicate entry")));
        /* 실패 이력은 롤백된 트랜잭션 밖에서 남겨야 한다. */
        assertEquals(List.of("BEGIN", "ROLLBACK"), timeline);
    }

    @Test
    @DisplayName("은행 입금(01)은 IN/INCOME, 출금(02)은 OUT/CONSUMPTION 으로 저장한다")
    void bankTransTypeCodeDrivesDirectionAndClassification() {
        when(client.getBankTransactions(eq("1"), eq(101L))).thenReturn(List.of(
                BankTransactionSyncDto.builder()
                        .transactionId(9001L).transactedAt("2026-08-10T09:05:01+09:00")
                        .transTypeCode("01").amount(new BigDecimal("2500000")).description("급여")
                        .build(),
                BankTransactionSyncDto.builder()
                        .transactionId(9002L).transactedAt("2026-08-11T09:05:01+09:00")
                        .transTypeCode("02").amount(new BigDecimal("15000")).description("체크카드 출금")
                        .build()));

        service.sync(1L);

        /* 급여 입금을 CONSUMPTION 으로 넣으면 미션·리포트의 소비 집계가 통째로 오염된다. */
        verify(transactionMapper).insert(argThat(t ->
                "급여".equals(t.getMerchantName())
                        && "IN".equals(t.getDirection())
                        && "INCOME".equals(t.getClassification())));
        verify(transactionMapper).insert(argThat(t ->
                "체크카드 출금".equals(t.getMerchantName())
                        && "OUT".equals(t.getDirection())
                        && "CONSUMPTION".equals(t.getClassification())));
    }

    @Test
    @DisplayName("모르는 거래유형 코드는 지어내지 않는다 — direction 은 비우고 TRANSFER 로 둔다")
    void unknownBankTransTypeCodeFallsBackToTransfer() {
        when(client.getBankTransactions(eq("1"), eq(101L))).thenReturn(List.of(
                BankTransactionSyncDto.builder()
                        .transactionId(9003L).transactedAt("2026-08-10T09:05:01+09:00")
                        .transTypeCode("99").amount(new BigDecimal("1000")).description("정체불명")
                        .build()));

        service.sync(1L);

        /* classification 은 NOT NULL + CHECK 라 비울 수 없다 — 소비도 수입도 아닌 값을 고른다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getDirection() == null && "TRANSFER".equals(t.getClassification())));
    }

    @Test
    @DisplayName("예적금·증권 거래는 소비가 아니라 TRANSFER 다")
    void depositAndSecuritiesAreTransfer() {
        when(client.getDeposits("1")).thenReturn(List.of(
                DepositSyncDto.builder().depositAccountId(201L).institutionCode("0004")
                        .institutionName("KB국민은행").productName("KB 적금")
                        .accountNoMasked("110-***-999999").balance(new BigDecimal("1000000")).build()));
        when(client.getDepositTransactions(eq("1"), eq(201L))).thenReturn(List.of(
                DepositTransactionSyncDto.builder()
                        .transactionId(7001L).transactedAt("2026-08-05T09:00:00+09:00")
                        .transTypeCode("01").amount(new BigDecimal("300000")).description("납입")
                        .build()));
        when(client.getStockAsset("1")).thenReturn(
                StockAssetSyncDto.builder().accountId(301L).institutionName("KB증권")
                        .currency("KRW").cashBalance(new BigDecimal("50000")).build());
        when(client.getSecuritiesTransactions(eq("1"), eq(301L))).thenReturn(List.of(
                SecuritiesTransactionSyncDto.builder()
                        .transactionId(8001L).transactedAt("2026-08-06T10:00:00+09:00")
                        .transTypeCode("02").securityProductName("삼성전자")
                        .transactionAmount(new BigDecimal("700000")).build()));

        service.sync(1L);

        /* 적금 납입·주식 매도 대금을 CONSUMPTION 으로 잡으면 지출이 없는 돈까지 부풀려진다. */
        verify(transactionMapper).insert(argThat(t ->
                "DEPOSIT".equals(t.getSourceType()) && "TRANSFER".equals(t.getClassification())));
        verify(transactionMapper).insert(argThat(t ->
                "SECURITIES".equals(t.getSourceType()) && "TRANSFER".equals(t.getClassification())));
    }

    @Test
    @DisplayName("페이머니는 충전(01)=TRANSFER, 결제(02)=CONSUMPTION, 환불(03)=is_refund 로 가른다")
    void payMoneyTransTypeCodeDrivesClassification() {
        when(client.getPayMoney("1")).thenReturn(List.of(
                PayMoneySyncDto.builder().payMoneyId(401L).providerCode("KAKAO")
                        .providerName("카카오페이").walletName("카카오페이 머니")
                        .balance(new BigDecimal("130000")).build()));
        when(client.getPayMoneyTransactions(eq("1"), eq(401L))).thenReturn(List.of(
                /* 충전은 양수, 결제는 **음수**, 환불은 양수로 내려온다(seed-v2.sql). */
                PayMoneyTransactionSyncDto.builder()
                        .transactionId(6001L).transactedAt("2026-06-12T18:00:01+09:00")
                        .transTypeCode("01").amount(new BigDecimal("150000")).build(),
                PayMoneyTransactionSyncDto.builder()
                        .transactionId(6002L).transactedAt("2026-06-15T12:30:00+09:00")
                        .transTypeCode("02").amount(new BigDecimal("-70000"))
                        .merchantName("배달의민족").build(),
                PayMoneyTransactionSyncDto.builder()
                        .transactionId(6003L).transactedAt("2026-06-27T10:00:00+09:00")
                        .transTypeCode("03").amount(new BigDecimal("10000"))
                        .merchantName("배달의민족").build()));

        service.sync(1L);

        /* 충전은 내 돈을 지갑으로 옮긴 것뿐이다 — 소비로 잡으면 지출이 통째로 부풀려진다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getCodefTrKey().endsWith("-6001")
                        && "TRANSFER".equals(t.getClassification())
                        && !t.isRefund()));
        /* 결제는 음수로 오지만 저장은 양수 + CONSUMPTION 이다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getCodefTrKey().endsWith("-6002")
                        && "CONSUMPTION".equals(t.getClassification())
                        && new BigDecimal("70000").compareTo(t.getAmount()) == 0
                        && !t.isRefund()));
        /* 환불은 CONSUMPTION + is_refund — 미션 집계가 -refunded_amount 로 상계한다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getCodefTrKey().endsWith("-6003")
                        && "CONSUMPTION".equals(t.getClassification())
                        && t.isRefund()
                        && new BigDecimal("10000").compareTo(t.getRefundedAmount()) == 0));
    }

    @Test
    @DisplayName("카드 취소(03)는 음수 금액을 양수 + is_refund=1 로 저장한다")
    void cardCancellationIsStoredAsPositiveRefund() {
        when(client.getCards("1")).thenReturn(List.of(
                CardSyncDto.builder().cardId(1L).cardNoMasked("9490-****-****-2201")
                        .cardTypeCode("01").currency("KRW").build()));
        when(client.getCardApprovals(eq("1"), eq(1L))).thenReturn(List.of(
                CardApprovalSyncDto.builder().approvalId(1L).approvalNo("N2-C-0814")
                        .approvedAt("2026-08-14T18:10:00+09:00").approvalTypeCode("01")
                        .merchantName("이마트 역삼점").approvedAmount(new BigDecimal("100000"))
                        .rawJson(null).build(),
                CardApprovalSyncDto.builder().approvalId(2L).approvalNo("N2-C-0814-C")
                        .approvedAt("2026-08-15T09:00:00+09:00").approvalTypeCode("03")
                        .merchantName("이마트 역삼점").approvedAmount(new BigDecimal("-20000"))
                        .rawJson("{\"originalApprovalNo\":\"N2-C-0814\"}").build()));
        when(client.getCardBills(anyString(), anyLong())).thenReturn(List.of());
        when(cardMapper.update(any())).thenReturn(0);

        service.sync(1L);

        /* amount 는 "항상 양수. 환불도 양수 + is_refund=1" 이 스키마 규약이다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getCodefTrKey().endsWith("N2-C-0814-C")
                        && new BigDecimal("20000").compareTo(t.getAmount()) == 0
                        && t.isRefund()
                        && new BigDecimal("20000").compareTo(t.getRefundedAmount()) == 0
                        && "N2-C-0814".equals(t.getOriginalApprovalNo())));
        /* 정상 승인은 환불 플래그가 서면 안 된다. */
        verify(transactionMapper).insert(argThat(t ->
                t.getCodefTrKey().endsWith("N2-C-0814")
                        && !t.isRefund() && t.getRefundedAmount() == null
                        && "OUT".equals(t.getDirection())
                        && "CONSUMPTION".equals(t.getClassification())));
    }

    @Test
    @DisplayName("동시 동기화가 같은 행을 먼저 넣어도 500 이 아니라 update 로 흡수한다")
    void concurrentInsertRaceFallsBackToUpdate() {
        /* update 0행 → insert 시도 → 상대가 이미 넣어 UNIQUE 위반. 여기서 터지면 사용자에게 500 이다. */
        when(transactionMapper.update(any())).thenReturn(0).thenReturn(1);
        when(transactionMapper.insert(any())).thenThrow(
                new DuplicateKeyException("Duplicate entry 'BANK-1-9001' for key 'uk_tx_codef_key'"));

        FinancialSyncResultDto result = service.sync(1L);

        assertEquals("COMPLETED", result.getStatus());
        /* 경합에서 진 쪽은 상대 행을 우리 값으로 갱신하고 넘어간다 (update 2회: 최초 시도 + 재시도). */
        verify(transactionMapper, times(2)).update(any());
        assertEquals(List.of("BEGIN", "COMMIT"), timeline);
    }

    @Test
    @DisplayName("저장 후 upsert 된 거래 id 로 규칙 카테고리화를 호출하고, 결과를 응답에 담는다")
    void callsRuleCategorizationWithUpsertedIdsAndReflectsResultInResponse() {
        when(transactionCategorizationService.categorizeRuleBased(eq(1L), anyList()))
                .thenReturn(RuleCategorizationResultDto.builder()
                        .ruleCategorizedCount(1)
                        .llmEligibleTransactionIds(List.of())
                        .build());

        FinancialSyncResultDto result = service.sync(1L);

        assertEquals(1, result.getCollectedTransactionCount());
        assertEquals(1, result.getRuleCategorizedCount());
        assertEquals(0, result.getLlmPendingTransactionCount());
        assertEquals("NOT_REQUIRED", result.getLlmCategorizationStatus());
        verify(transactionCategorizationService).categorizeRuleBased(eq(1L), anyList());
    }

    @Test
    @DisplayName("규칙으로 못 채운 거래가 있으면 LlmCategorizationRequestedEvent 를 발행하고 상태를 PENDING 으로 둔다")
    void publishesLlmEventWhenEligibleTransactionsRemain() {
        when(transactionCategorizationService.categorizeRuleBased(eq(1L), anyList()))
                .thenReturn(RuleCategorizationResultDto.builder()
                        .ruleCategorizedCount(0)
                        .llmEligibleTransactionIds(List.of(999L))
                        .build());

        FinancialSyncResultDto result = service.sync(1L);

        assertEquals(1, result.getLlmPendingTransactionCount());
        assertEquals("PENDING", result.getLlmCategorizationStatus());
        /*
         * ApplicationEventPublisher 는 publishEvent(ApplicationEvent)/publishEvent(Object) 오버로드가
         * 둘 다 있다. argThat(람다) 를 타입 힌트 없이 쓰면 javac 가 더 구체적인 ApplicationEvent 오버로드로
         * 고정해버려, 그와 무관한 record 타입(LlmCategorizationRequestedEvent) 로의 instanceof/캐스팅이
         * 컴파일 에러가 된다. Object 로 타입 힌트를 줘 Object 오버로드를 타게 한다.
         */
        verify(eventPublisher).publishEvent(Mockito.<Object>argThat(event ->
                event instanceof LlmCategorizationRequestedEvent
                        && ((LlmCategorizationRequestedEvent) event).userId().equals(1L)
                        && ((LlmCategorizationRequestedEvent) event).transactionIds().equals(List.of(999L))));
    }

    @Test
    @DisplayName("LLM 대상이 없으면 이벤트를 발행하지 않는다")
    void doesNotPublishEventWhenNoLlmEligibleTransactions() {
        service.sync(1L);

        verify(eventPublisher, never()).publishEvent(any(LlmCategorizationRequestedEvent.class));
    }
}

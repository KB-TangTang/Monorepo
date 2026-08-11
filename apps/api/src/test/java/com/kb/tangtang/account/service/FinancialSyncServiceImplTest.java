package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.sync.FinancialSyncClient;
import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
import com.kb.tangtang.account.client.sync.dto.BankAccountSyncDto;
import com.kb.tangtang.account.client.sync.dto.BankTransactionSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardApprovalSyncDto;
import com.kb.tangtang.account.client.sync.dto.CardSyncDto;
import com.kb.tangtang.account.domain.Card;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.mapper.CardBillMapper;
import com.kb.tangtang.account.mapper.CardMapper;
import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import com.kb.tangtang.account.mapper.FinancialSyncHistoryMapper;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private FinancialSyncClient client;
    private ScenarioKeyProvider scenarioKeyProvider;
    private ConnectedAccountMapper connectedAccountMapper;
    private LoanMapper loanMapper;
    private InvestmentHoldingMapper investmentHoldingMapper;
    private CardMapper cardMapper;
    private CardBillMapper cardBillMapper;
    private TransactionMapper transactionMapper;
    private FinancialSyncHistoryMapper syncHistoryMapper;
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

        Clock clock = Clock.fixed(Instant.parse("2026-08-12T01:00:00Z"), ZoneId.of("Asia/Seoul"));

        service = new FinancialSyncServiceImpl(
                client, scenarioKeyProvider, connectedAccountMapper, loanMapper,
                investmentHoldingMapper, cardMapper, cardBillMapper, transactionMapper,
                syncHistoryMapper, clock);

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

        when(connectedAccountMapper.updateSynced(anyLong(), eq(1L), any(), any())).thenReturn(0);
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
}

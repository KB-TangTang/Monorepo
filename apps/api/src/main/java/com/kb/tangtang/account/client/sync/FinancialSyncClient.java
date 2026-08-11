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

import java.util.List;

/**
 * 금융 동기화(이슈 #147) 전용 목서버 클라이언트.
 *
 * ⚠ account.client.FinancialDataClient 와 다르다 — 그건 계좌연동 "인증" 흐름
 *   (auth-methods/connection/extra-auth)용이고, 이건 /api/v1/assets/* 데이터 풀(pull) 전용이다.
 */
public interface FinancialSyncClient {
    List<BankAccountSyncDto> getBankAccounts(String scenarioKey);
    List<BankTransactionSyncDto> getBankTransactions(String scenarioKey, long accountId);
    List<DepositSyncDto> getDeposits(String scenarioKey);
    List<DepositTransactionSyncDto> getDepositTransactions(String scenarioKey, long depositAccountId);
    StockAssetSyncDto getStockAsset(String scenarioKey);
    List<SecuritiesTransactionSyncDto> getSecuritiesTransactions(String scenarioKey, long accountId);
    List<LoanSyncDto> getLoans(String scenarioKey);
    List<LoanTransactionSyncDto> getLoanTransactions(String scenarioKey, long loanId);
    List<PayMoneySyncDto> getPayMoney(String scenarioKey);
    List<PayMoneyTransactionSyncDto> getPayMoneyTransactions(String scenarioKey, long payMoneyId);
    List<CardSyncDto> getCards(String scenarioKey);
    List<CardApprovalSyncDto> getCardApprovals(String scenarioKey, long cardId);
    List<CardBillSyncDto> getCardBills(String scenarioKey, long cardId);
}

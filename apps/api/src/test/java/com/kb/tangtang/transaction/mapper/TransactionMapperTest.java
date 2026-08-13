package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Transaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("실 DB 연결 필요 — 로컬 수동 검증용")
@SpringJUnitConfig
@ContextConfiguration(classes = com.kb.tangtang.config.RootConfig.class)
class TransactionMapperTest {

    @Autowired
    private TransactionMapper transactionMapper;

    @Test
    void insertThenUpdateIsIdempotent() {
        Transaction tx = Transaction.builder()
                .userId(1L)
                .accountId(1L)
                .codefTrKey("BANK-1-TXN-TEST-001")
                .amount(new BigDecimal("10000"))
                .direction("OUT")
                .trDate(LocalDate.now())
                .classification("CONSUMPTION")
                .isExcludedFromSummary(false)
                .sourceType("BANK")
                .build();

        int updated = transactionMapper.update(tx);
        assertEquals(0, updated);

        int inserted = transactionMapper.insert(tx);
        assertEquals(1, inserted);

        int updatedAgain = transactionMapper.update(tx);
        assertEquals(1, updatedAgain, "같은 codefTrKey 로 재동기화하면 insert 가 아니라 update 여야 한다");
    }

    @Test
    void updateAppliesCategorySource() {
        Transaction tx = Transaction.builder()
                .userId(1L)
                .accountId(1L)
                .codefTrKey("BANK-1-TXN-TEST-002")
                .amount(new BigDecimal("10000"))
                .direction("OUT")
                .trDate(LocalDate.now())
                .classification("CONSUMPTION")
                .isExcludedFromSummary(false)
                .sourceType("BANK")
                .build();
        transactionMapper.insert(tx);

        tx.setCategoryId(1L);
        tx.setCategorySource("USER");
        int updated = transactionMapper.update(tx);

        assertEquals(1, updated, "category_id·category_source 를 채운 update 도 정상 반영돼야 한다");
        // ck_tx_category_source_requires_id 위반 확인 — categoryId 없이 categorySource 만 채우면
        // DB CHECK 제약(20260813_add_transaction_category_source.sql)에 걸려 예외가 나야 한다.
    }

    /**
     * 이슈 #147 리뷰에서 발견 — findEligibleForRuleCategorization 이 예전엔 USER 소스만 제외해서,
     * RULE_MCC/RULE_KEYWORD/LLM 으로 이미 분류된 거래도 재동기화마다 다시 대상에 포함되고
     * ruleCategorizedCount 가 실제로는 새로 분류한 게 없는데도 매번 부풀려졌다. 이제는
     * category_source IS NULL 인 거래만 대상이어야 한다 — RULE_KEYWORD 로 이미 분류된 거래는
     * 빠져야 하고, 아직 미분류(NULL)인 거래는 남아야 한다.
     */
    @Test
    void eligibleForRuleCategorizationExcludesAlreadyClassifiedTransactions() {
        Transaction alreadyClassified = Transaction.builder()
                .userId(1L)
                .accountId(1L)
                .codefTrKey("BANK-1-TXN-TEST-003")
                .amount(new BigDecimal("10000"))
                .direction("OUT")
                .trDate(LocalDate.now())
                .classification("CONSUMPTION")
                .isExcludedFromSummary(false)
                .sourceType("BANK")
                .build();
        transactionMapper.insert(alreadyClassified);
        alreadyClassified.setCategoryId(1L);
        alreadyClassified.setCategorySource("RULE_KEYWORD");
        transactionMapper.update(alreadyClassified);

        Transaction stillUnclassified = Transaction.builder()
                .userId(1L)
                .accountId(1L)
                .codefTrKey("BANK-1-TXN-TEST-004")
                .amount(new BigDecimal("20000"))
                .direction("OUT")
                .trDate(LocalDate.now())
                .classification("CONSUMPTION")
                .isExcludedFromSummary(false)
                .sourceType("BANK")
                .build();
        transactionMapper.insert(stillUnclassified);

        List<Transaction> eligible = transactionMapper.findEligibleForRuleCategorization(
                1L, List.of(alreadyClassified.getId(), stillUnclassified.getId()));

        List<Long> eligibleIds = eligible.stream().map(Transaction::getId).toList();
        assertFalse(eligibleIds.contains(alreadyClassified.getId()),
                "RULE_KEYWORD 로 이미 분류된 거래는 재선택 대상에서 빠져야 한다");
        assertTrue(eligibleIds.contains(stillUnclassified.getId()),
                "아직 미분류(category_source NULL)인 거래는 대상이어야 한다");
    }
}

package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Transaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

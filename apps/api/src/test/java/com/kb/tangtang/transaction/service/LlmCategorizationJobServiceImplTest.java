package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import com.kb.tangtang.transaction.domain.LlmCategorizationJobItem;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobItemMapper;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCategorizationJobServiceImplTest {

    private TransactionMapper transactionMapper;
    private LlmCategorizationJobMapper jobMapper;
    private LlmCategorizationJobItemMapper jobItemMapper;
    private LlmCategorizationJobServiceImpl service;

    @BeforeEach
    void setUp() {
        transactionMapper = mock(TransactionMapper.class);
        jobMapper = mock(LlmCategorizationJobMapper.class);
        jobItemMapper = mock(LlmCategorizationJobItemMapper.class);
        service = new LlmCategorizationJobServiceImpl(transactionMapper, jobMapper, jobItemMapper);
    }

    @Test
    @DisplayName("빈 목록이면 아무 매퍼도 호출하지 않는다")
    void emptyListDoesNothing() {
        service.registerPendingJobs(1L, List.of());

        verify(transactionMapper, times(0)).findByIds(anyList());
    }

    @Test
    @DisplayName("null 목록이면 아무 매퍼도 호출하지 않는다")
    void nullListDoesNothing() {
        service.registerPendingJobs(1L, null);

        verify(transactionMapper, times(0)).findByIds(anyList());
    }

    @Test
    @DisplayName("25건을 넣으면 20건·5건 두 작업으로 나뉜다")
    void splitsIntoTwentyItemBatches() {
        List<Long> ids = LongStream.rangeClosed(1, 25).boxed().collect(Collectors.toList());
        List<Transaction> txs = ids.stream()
                .map(id -> Transaction.builder().id(id).trDate(LocalDate.of(2026, 1, 1).plusDays(id)).build())
                .collect(Collectors.toList());
        when(transactionMapper.findByIds(ids)).thenReturn(txs);

        service.registerPendingJobs(1L, ids);

        ArgumentCaptor<LlmCategorizationJob> jobCaptor = ArgumentCaptor.forClass(LlmCategorizationJob.class);
        verify(jobMapper, times(2)).insert(jobCaptor.capture());
        assertEquals(List.of(20, 5),
                jobCaptor.getAllValues().stream()
                        .map(LlmCategorizationJob::getTransactionCount)
                        .collect(Collectors.toList()));
        verify(jobItemMapper, times(25)).insert(any(LlmCategorizationJobItem.class));
    }

    @Test
    @DisplayName("transaction_date 오름차순으로 정렬해서 배치를 만든다")
    void ordersByTransactionDateAscending() {
        Transaction late = Transaction.builder().id(100L).trDate(LocalDate.of(2026, 8, 10)).build();
        Transaction early = Transaction.builder().id(200L).trDate(LocalDate.of(2026, 1, 1)).build();
        when(transactionMapper.findByIds(List.of(100L, 200L))).thenReturn(List.of(late, early));

        List<LlmCategorizationJobItem> captured = new ArrayList<>();
        when(jobItemMapper.insert(any(LlmCategorizationJobItem.class))).thenAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return 1;
        });

        service.registerPendingJobs(1L, List.of(100L, 200L));

        assertEquals(List.of(200L, 100L),
                captured.stream().map(LlmCategorizationJobItem::getTransactionId).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("이미 다른 작업에 등록된 거래(UNIQUE 위반)는 건너뛰고 나머지는 계속 등록한다")
    void duplicateItemIsSkippedNotFatal() {
        Transaction tx1 = Transaction.builder().id(1L).trDate(LocalDate.of(2026, 1, 1)).build();
        Transaction tx2 = Transaction.builder().id(2L).trDate(LocalDate.of(2026, 1, 2)).build();
        when(transactionMapper.findByIds(List.of(1L, 2L))).thenReturn(List.of(tx1, tx2));
        when(jobItemMapper.insert(any(LlmCategorizationJobItem.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_lcji_transaction'"))
                .thenReturn(1);

        service.registerPendingJobs(1L, List.of(1L, 2L));

        verify(jobItemMapper, times(2)).insert(any(LlmCategorizationJobItem.class));
        verify(jobMapper, times(1)).insert(any(LlmCategorizationJob.class));
    }
}

package com.kb.tangtang.transaction.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.mapper.CategoryMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import com.kb.tangtang.transaction.mapper.UserCategoryMapMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private static final long USER_ID = 1L;
    private static final long TRANSACTION_ID = 501L;
    private static final long CATEGORY_ID = 12L;

    private TransactionMapper transactionMapper;
    private CategoryMapper categoryMapper;
    private UserCategoryMapMapper userCategoryMapMapper;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        transactionMapper = mock(TransactionMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        userCategoryMapMapper = mock(UserCategoryMapMapper.class);
        service = new TransactionService(transactionMapper, categoryMapper, userCategoryMapMapper);
    }

    private Transaction ownedTransaction() {
        return Transaction.builder()
                .id(TRANSACTION_ID).userId(USER_ID).merchantName("스타벅스 강남점")
                .build();
    }

    @Test
    @DisplayName("applyToMerchant=false면 거래 한 건만 USER로 바뀌고 가맹점 규칙은 upsert하지 않는다")
    void updatesSingleTransactionOnly() {
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(ownedTransaction());
        when(categoryMapper.findById(CATEGORY_ID)).thenReturn(Category.builder().id(CATEGORY_ID).build());

        TransactionCategoryUpdateResultDto result =
                service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, false);

        assertEquals(TRANSACTION_ID, result.getTransactionId());
        assertEquals(CATEGORY_ID, result.getCategoryId());
        assertEquals("USER", result.getCategorySource());
        assertFalse(result.isMerchantRuleApplied());
        verify(transactionMapper).updateCategoryByUser(TRANSACTION_ID, USER_ID, CATEGORY_ID);
        verify(userCategoryMapMapper, never()).upsert(anyLong(), any(String.class), any());
    }

    @Test
    @DisplayName("applyToMerchant=true면 정규화된 가맹점명으로 가맹점 규칙도 upsert한다")
    void appliesMerchantRuleWithNormalizedName() {
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(ownedTransaction());
        when(categoryMapper.findById(CATEGORY_ID)).thenReturn(Category.builder().id(CATEGORY_ID).build());

        TransactionCategoryUpdateResultDto result =
                service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, true);

        assertTrue(result.isMerchantRuleApplied());
        verify(userCategoryMapMapper).upsert(USER_ID, "스타벅스강남점", CATEGORY_ID);
    }

    @Test
    @DisplayName("없는 거래·타인 거래는 NOT_FOUND, 카테고리 매핑은 시도하지 않는다")
    void missingOrUnownedTransactionIsNotFound() {
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, false));

        assertEquals("NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(transactionMapper, never()).updateCategoryByUser(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("존재하지 않는 categoryId는 CATEGORY_NOT_FOUND다")
    void missingCategoryIsCategoryNotFound() {
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(ownedTransaction());
        when(categoryMapper.findById(CATEGORY_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, false));

        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(transactionMapper, never()).updateCategoryByUser(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("categoryId가 없으면 조회 없이 INVALID_REQUEST다")
    void missingCategoryIdIsInvalidRequest() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateCategory(USER_ID, TRANSACTION_ID, null, false));

        assertEquals("INVALID_REQUEST", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(transactionMapper, never()).findByIdAndUser(anyLong(), anyLong());
    }

    @Test
    @DisplayName("applyToMerchant=true인데 가맹점명이 없으면 MERCHANT_NAME_REQUIRED다")
    void merchantNameRequiredWhenApplyingMerchantRule() {
        Transaction transactionWithoutMerchant = Transaction.builder()
                .id(TRANSACTION_ID).userId(USER_ID).merchantName(null)
                .build();
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(transactionWithoutMerchant);
        when(categoryMapper.findById(CATEGORY_ID)).thenReturn(Category.builder().id(CATEGORY_ID).build());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, true));

        assertEquals("MERCHANT_NAME_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(transactionMapper, never()).updateCategoryByUser(anyLong(), anyLong(), any());
        verify(userCategoryMapMapper, never()).upsert(anyLong(), any(String.class), any());
    }

    @Test
    @DisplayName("applyToMerchant=true인데 가맹점명이 정규화하면 빈 문자열이면 MERCHANT_NAME_REQUIRED다")
    void merchantNameRequiredWhenNormalizedNameIsEmpty() {
        Transaction transactionWithBlankMerchant = Transaction.builder()
                .id(TRANSACTION_ID).userId(USER_ID).merchantName("(주)")
                .build();
        when(transactionMapper.findByIdAndUser(TRANSACTION_ID, USER_ID)).thenReturn(transactionWithBlankMerchant);
        when(categoryMapper.findById(CATEGORY_ID)).thenReturn(Category.builder().id(CATEGORY_ID).build());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateCategory(USER_ID, TRANSACTION_ID, CATEGORY_ID, true));

        assertEquals("MERCHANT_NAME_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(transactionMapper, never()).updateCategoryByUser(anyLong(), anyLong(), any());
        verify(userCategoryMapMapper, never()).upsert(anyLong(), any(String.class), any());
    }
}

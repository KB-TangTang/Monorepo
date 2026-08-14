package com.kb.tangtang.transaction.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.CategorySource;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.mapper.CategoryMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import com.kb.tangtang.transaction.mapper.UserCategoryMapMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자가 거래의 카테고리를 직접 수정한다. 자동 분류(TransactionCategorizationService)와는
 * 별개의 쓰기 경로다 — 이슈 #237 설계 문서 참고.
 */
@Service
public class TransactionService {

    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final UserCategoryMapMapper userCategoryMapMapper;

    public TransactionService(TransactionMapper transactionMapper,
                               CategoryMapper categoryMapper,
                               UserCategoryMapMapper userCategoryMapMapper) {
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
        this.userCategoryMapMapper = userCategoryMapMapper;
    }

    @Transactional
    public TransactionCategoryUpdateResultDto updateCategory(long userId, long transactionId,
                                                               Long categoryId, boolean applyToMerchant) {
        if (categoryId == null) {
            throw new BusinessException("INVALID_REQUEST", "categoryId는 필수입니다.");
        }

        Transaction transaction = transactionMapper.findByIdAndUser(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException("NOT_FOUND", "거래를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        Category category = categoryMapper.findById(categoryId);
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        transactionMapper.updateCategoryByUser(transactionId, userId, categoryId);

        if (applyToMerchant) {
            if (transaction.getMerchantName() == null) {
                throw new BusinessException("MERCHANT_NAME_REQUIRED", "가맹점명이 없는 거래는 가맹점 규칙을 적용할 수 없습니다.");
            }
            String normalized = MerchantNameNormalizer.normalize(transaction.getMerchantName());
            userCategoryMapMapper.upsert(userId, normalized, categoryId);
        }

        return TransactionCategoryUpdateResultDto.builder()
                .transactionId(transactionId)
                .categoryId(categoryId)
                .categorySource(CategorySource.USER)
                .merchantRuleApplied(applyToMerchant)
                .build();
    }
}

package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.CategorySource;
import com.kb.tangtang.transaction.domain.MerchantCategoryMap;
import com.kb.tangtang.transaction.domain.MerchantKeywordRule;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.domain.UserCategoryMap;
import com.kb.tangtang.transaction.dto.RuleCategorizationResultDto;
import com.kb.tangtang.transaction.mapper.MerchantCategoryMapMapper;
import com.kb.tangtang.transaction.mapper.MerchantKeywordRuleMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import com.kb.tangtang.transaction.mapper.UserCategoryMapMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 이슈 #147 — 규칙 1~4단계 카테고리화.
 *
 * account 모듈(FinancialSyncServiceImpl)이 동기 직접 호출한다. 모듈 경계 원칙("모듈 간 직접 Service
 * 호출 최소화")의 명시적 예외다 — ruleCategorizedCount 를 같은 HTTP 응답에 담아야 해서 이벤트로는
 * 불가능하다(계획 문서 Global Constraints 참고).
 */
@Service
public class TransactionCategorizationServiceImpl implements TransactionCategorizationService {

    private final TransactionMapper transactionMapper;
    private final UserCategoryMapMapper userCategoryMapMapper;
    private final MerchantCategoryMapMapper merchantCategoryMapMapper;
    private final MerchantKeywordRuleMapper merchantKeywordRuleMapper;

    public TransactionCategorizationServiceImpl(TransactionMapper transactionMapper,
                                                 UserCategoryMapMapper userCategoryMapMapper,
                                                 MerchantCategoryMapMapper merchantCategoryMapMapper,
                                                 MerchantKeywordRuleMapper merchantKeywordRuleMapper) {
        this.transactionMapper = transactionMapper;
        this.userCategoryMapMapper = userCategoryMapMapper;
        this.merchantCategoryMapMapper = merchantCategoryMapMapper;
        this.merchantKeywordRuleMapper = merchantKeywordRuleMapper;
    }

    @Override
    @Transactional
    public RuleCategorizationResultDto categorizeRuleBased(long userId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return RuleCategorizationResultDto.builder()
                    .ruleCategorizedCount(0)
                    .llmEligibleTransactionIds(List.of())
                    .build();
        }

        List<Transaction> eligible = transactionMapper.findEligibleForRuleCategorization(userId, transactionIds);

        int ruleCategorizedCount = 0;
        List<Long> llmEligibleIds = new ArrayList<>();

        for (Transaction tx : eligible) {
            ClassificationResult result = classify(userId, tx);
            if (result != null) {
                int affected = transactionMapper.updateCategory(tx.getId(), result.categoryId, result.categorySource);
                if (affected > 0) {
                    ruleCategorizedCount++;
                }
            } else if (!tx.isRefund()) {
                /*
                 * 환불 거래는 원거래 카테고리 계승을 아직 구현하지 않았다(계획 문서 참고 — 후속 작업).
                 * 대신 일반 거래와 동일하게 규칙 1~4단계를 적용하고, 그마저 다 미스하면 LLM 대상에서는
                 * 제외한다("환불 거래는 원거래를 계승할 수 있을 때만 계승하고, 그렇지 않으면 LLM 분류
                 * 대상에 넣지 않는다"는 스펙을 이 형태로 만족한다).
                 */
                llmEligibleIds.add(tx.getId());
            }
        }

        return RuleCategorizationResultDto.builder()
                .ruleCategorizedCount(ruleCategorizedCount)
                .llmEligibleTransactionIds(llmEligibleIds)
                .build();
    }

    /** 우선순위 1~4단계. 매칭되면 category_id·category_source 를, 못 찾으면 null 을 돌려준다. */
    private ClassificationResult classify(long userId, Transaction tx) {
        String normalizedMerchant = MerchantNameNormalizer.normalize(tx.getMerchantName());

        if (normalizedMerchant != null && !normalizedMerchant.isEmpty()) {
            UserCategoryMap userMap = userCategoryMapMapper.findByUserAndMerchant(userId, normalizedMerchant);
            if (userMap != null) {
                return new ClassificationResult(userMap.getCategoryId(), CategorySource.USER);
            }

            MerchantCategoryMap merchantMap = merchantCategoryMapMapper.findByMerchantNameNormalized(normalizedMerchant);
            if (merchantMap != null) {
                return new ClassificationResult(merchantMap.getCategoryId(), translateSource(merchantMap.getSource()));
            }
        }

        String normalizedMcc = MerchantNameNormalizer.normalize(tx.getMerchantCategoryName());
        if (normalizedMcc != null && !normalizedMcc.isEmpty()) {
            /* 3단계는 몇 번째 우선순위로 찾았든 항상 RULE_MCC 로 저장한다 — 행의 source 컬럼값과 무관하다. */
            MerchantCategoryMap mccMap = merchantCategoryMapMapper.findByMerchantNameNormalized(normalizedMcc);
            if (mccMap != null) {
                return new ClassificationResult(mccMap.getCategoryId(), CategorySource.RULE_MCC);
            }
        }

        String haystack = buildKeywordHaystack(tx);
        if (!haystack.isEmpty()) {
            MerchantKeywordRule rule = merchantKeywordRuleMapper.findBestMatch(haystack);
            if (rule != null) {
                return new ClassificationResult(rule.getCategoryId(), CategorySource.RULE_KEYWORD);
            }
        }

        return null;
    }

    /** merchant_name_normalized, merchant_name, description1 을 정규화해 이어붙인다(4단계 대상 텍스트). */
    private static String buildKeywordHaystack(Transaction tx) {
        StringBuilder sb = new StringBuilder();
        appendNormalized(sb, tx.getMerchantNameNormalized());
        appendNormalized(sb, tx.getMerchantName());
        appendNormalized(sb, tx.getDescription1());
        return sb.toString();
    }

    private static void appendNormalized(StringBuilder sb, String raw) {
        String normalized = MerchantNameNormalizer.normalize(raw);
        if (normalized != null) {
            sb.append(normalized);
        }
    }

    private static String translateSource(String merchantCategoryMapSource) {
        switch (merchantCategoryMapSource) {
            case "MCC":
                return CategorySource.RULE_MCC;
            case "KEYWORD":
                return CategorySource.RULE_KEYWORD;
            case "LLM":
                return CategorySource.LLM;
            default:
                /* ck_mcm_source CHECK 제약이 이 셋만 허용한다 — 여기 오면 DB 와 코드가 어긋난 것. */
                throw new IllegalStateException("알 수 없는 tbl_merchant_category_map.source: " + merchantCategoryMapSource);
        }
    }

    private static final class ClassificationResult {
        private final Long categoryId;
        private final String categorySource;

        private ClassificationResult(Long categoryId, String categorySource) {
            this.categoryId = categoryId;
            this.categorySource = categorySource;
        }
    }
}

package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.MerchantCategoryMap;
import com.kb.tangtang.transaction.domain.MerchantKeywordRule;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.domain.UserCategoryMap;
import com.kb.tangtang.transaction.dto.RuleCategorizationResultDto;
import com.kb.tangtang.transaction.mapper.MerchantCategoryMapMapper;
import com.kb.tangtang.transaction.mapper.MerchantKeywordRuleMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import com.kb.tangtang.transaction.mapper.UserCategoryMapMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionCategorizationServiceImplTest {

    private TransactionMapper transactionMapper;
    private UserCategoryMapMapper userCategoryMapMapper;
    private MerchantCategoryMapMapper merchantCategoryMapMapper;
    private MerchantKeywordRuleMapper merchantKeywordRuleMapper;
    private TransactionCategorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        transactionMapper = mock(TransactionMapper.class);
        userCategoryMapMapper = mock(UserCategoryMapMapper.class);
        merchantCategoryMapMapper = mock(MerchantCategoryMapMapper.class);
        merchantKeywordRuleMapper = mock(MerchantKeywordRuleMapper.class);
        service = new TransactionCategorizationServiceImpl(
                transactionMapper, userCategoryMapMapper, merchantCategoryMapMapper, merchantKeywordRuleMapper);

        when(transactionMapper.updateCategory(any(), any(), any())).thenReturn(1);
    }

    private static Transaction consumption(Long id, String merchantName) {
        return Transaction.builder()
                .id(id).userId(1L).merchantName(merchantName).merchantNameNormalized(merchantName)
                .classification("CONSUMPTION").sourceType("CARD_CREDIT").isRefund(false)
                .build();
    }

    @Test
    @DisplayName("빈 id 목록이면 매퍼를 호출하지 않고 즉시 빈 결과를 돌려준다")
    void emptyIdsShortCircuits() {
        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of());

        assertEquals(0, result.getRuleCategorizedCount());
        assertTrue(result.getLlmEligibleTransactionIds().isEmpty());
        verify(transactionMapper, never()).findEligibleForRuleCategorization(anyLong(), anyList());
    }

    @Test
    @DisplayName("1순위: 사용자 가맹점 매핑이 있으면 USER 소스로 저장하고 규칙 분류 건수에 센다")
    void priority1UserMapWins() {
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(10L)))
                .thenReturn(List.of(consumption(10L, "스타벅스")));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any()))
                .thenReturn(UserCategoryMap.builder().userId(1L).categoryId(5L).build());

        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of(10L));

        assertEquals(1, result.getRuleCategorizedCount());
        assertTrue(result.getLlmEligibleTransactionIds().isEmpty());
        verify(transactionMapper).updateCategory(10L, 5L, "USER");
        verify(merchantCategoryMapMapper, never()).findByMerchantNameNormalized(any());
    }

    @Test
    @DisplayName("2순위: 공용 가맹점 매핑의 source='KEYWORD' 는 RULE_KEYWORD 로 변환해 저장한다")
    void priority2TranslatesSourceKeyword() {
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(11L)))
                .thenReturn(List.of(consumption(11L, "쿠팡이츠")));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized(any()))
                .thenReturn(MerchantCategoryMap.builder().categoryId(7L).source("KEYWORD").build());

        service.categorizeRuleBased(1L, List.of(11L));

        verify(transactionMapper).updateCategory(11L, 7L, "RULE_KEYWORD");
    }

    @Test
    @DisplayName("2순위: 공용 가맹점 매핑의 source='MCC' 는 RULE_MCC, 'LLM' 은 LLM 으로 변환한다")
    void priority2TranslatesSourceMccAndLlm() {
        when(transactionMapper.findEligibleForRuleCategorization(eq(1L), anyList()))
                .thenReturn(List.of(consumption(12L, "이마트"), consumption(13L, "배달의민족")));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("이마트"))
                .thenReturn(MerchantCategoryMap.builder().categoryId(1L).source("MCC").build());
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("배달의민족"))
                .thenReturn(MerchantCategoryMap.builder().categoryId(2L).source("LLM").build());

        service.categorizeRuleBased(1L, List.of(12L, 13L));

        verify(transactionMapper).updateCategory(12L, 1L, "RULE_MCC");
        verify(transactionMapper).updateCategory(13L, 2L, "LLM");
    }

    @Test
    @DisplayName("3순위: 가맹점명으로 못 찾으면 정규화된 업종명으로 재조회하고, 매칭되면 항상 RULE_MCC 다")
    void priority3MatchesByMerchantCategoryName() {
        Transaction tx = consumption(14L, "이름모를카페")
                .toBuilder().merchantCategoryName("커피전문점").build();
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(14L))).thenReturn(List.of(tx));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("이름모를카페")).thenReturn(null);
        /* 이 행의 source 컬럼이 실제로는 'KEYWORD' 여도, 3단계로 찾았다면 무조건 RULE_MCC 로 저장한다. */
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("커피전문점"))
                .thenReturn(MerchantCategoryMap.builder().categoryId(9L).source("KEYWORD").build());

        service.categorizeRuleBased(1L, List.of(14L));

        verify(transactionMapper).updateCategory(14L, 9L, "RULE_MCC");
    }

    @Test
    @DisplayName("4순위: 1~3순위가 다 미스하면 키워드 규칙(더 구체적인 것 우선)으로 분류한다")
    void priority4FallsBackToKeywordRule() {
        Transaction tx = consumption(15L, "쿠팡이츠 강남점");
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(15L))).thenReturn(List.of(tx));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized(any())).thenReturn(null);
        when(merchantKeywordRuleMapper.findBestMatch(any()))
                .thenReturn(MerchantKeywordRule.builder().categoryId(3L).keywordNormalized("쿠팡이츠").build());

        service.categorizeRuleBased(1L, List.of(15L));

        verify(transactionMapper).updateCategory(15L, 3L, "RULE_KEYWORD");
    }

    @Test
    @DisplayName("1~4순위가 전부 미스한 일반 소비 거래는 LLM 대상 목록에 담긴다")
    void unmatchedConsumptionGoesToLlmQueue() {
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(16L)))
                .thenReturn(List.of(consumption(16L, "정체불명상점")));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized(any())).thenReturn(null);
        when(merchantKeywordRuleMapper.findBestMatch(any())).thenReturn(null);

        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of(16L));

        assertEquals(0, result.getRuleCategorizedCount());
        assertEquals(List.of(16L), result.getLlmEligibleTransactionIds());
        verify(transactionMapper, never()).updateCategory(any(), any(), any());
    }

    @Test
    @DisplayName("환불 거래도 일반 거래와 동일하게 규칙을 적용하되, 전부 미스하면 LLM 대상에서 제외한다")
    void unmatchedRefundIsExcludedFromLlmQueue() {
        Transaction refund = consumption(17L, "정체불명상점").toBuilder().isRefund(true).build();
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(17L))).thenReturn(List.of(refund));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized(any())).thenReturn(null);
        when(merchantKeywordRuleMapper.findBestMatch(any())).thenReturn(null);

        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of(17L));

        assertEquals(0, result.getRuleCategorizedCount());
        assertTrue(result.getLlmEligibleTransactionIds().isEmpty());
        verify(transactionMapper, never()).updateCategory(any(), any(), any());
    }

    @Test
    @DisplayName("환불 거래가 규칙으로 매칭되면 일반 거래처럼 그대로 분류·카운트된다")
    void matchedRefundIsCategorizedNormally() {
        Transaction refund = consumption(18L, "이마트").toBuilder().isRefund(true).build();
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(18L))).thenReturn(List.of(refund));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("이마트"))
                .thenReturn(MerchantCategoryMap.builder().categoryId(4L).source("MCC").build());

        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of(18L));

        assertEquals(1, result.getRuleCategorizedCount());
        verify(transactionMapper).updateCategory(18L, 4L, "RULE_MCC");
    }

    @Test
    @DisplayName("updateCategory 가 가드에 걸려 0행을 반환하면(USER 로 바뀐 경합) 규칙 분류 건수에 세지 않는다")
    void guardedUpdateDoesNotCountAsRuleCategorized() {
        when(transactionMapper.findEligibleForRuleCategorization(1L, List.of(19L)))
                .thenReturn(List.of(consumption(19L, "이마트")));
        when(userCategoryMapMapper.findByUserAndMerchant(eq(1L), any())).thenReturn(null);
        when(merchantCategoryMapMapper.findByMerchantNameNormalized("이마트"))
                .thenReturn(MerchantCategoryMap.builder().categoryId(4L).source("MCC").build());
        when(transactionMapper.updateCategory(19L, 4L, "RULE_MCC")).thenReturn(0);

        RuleCategorizationResultDto result = service.categorizeRuleBased(1L, List.of(19L));

        assertEquals(0, result.getRuleCategorizedCount());
    }
}

package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionMapper {

    /** codef_tr_key(멱등키) 기준 update 시도. 0행이면 호출부가 insert 로 이어간다. */
    int update(Transaction transaction);

    int insert(Transaction transaction);

    /**
     * 이번 동기화 후처리: 아직 연결되지 않은 BANK/CARD_CHECK 쌍 중 correlation_id 가 같은 것을 찾아
     * linked_transaction_id 로 잇고, CARD_CHECK 쪽을 집계 기준으로 남긴 채 BANK 쪽을
     * is_excluded_from_summary=1 로 표시한다. 몇 번을 다시 돌려도 결과가 같다(멱등) —
     * WHERE 절이 linked_transaction_id IS NULL 인 것만 대상으로 삼기 때문이다.
     */
    int linkByCorrelation(@Param("userId") long userId);

    /**
     * 카테고리 갱신 전용. 재동기화용 update() 와 반드시 분리한다 — update() 는 원천 API 데이터로
     * 새로 조립한 Transaction(categoryId/categorySource=null)을 SET 하므로, 그 문으로 카테고리를
     * 건드리면 재동기화 때마다 이미 분류된 카테고리가 NULL 로 덮어써진다(이슈 #147 계획 문서).
     * WHERE 절의 category_source 가드가 사용자 지정 카테고리를 DB 레벨에서 보호한다.
     */
    int updateCategory(@Param("id") Long id, @Param("categoryId") Long categoryId,
                        @Param("categorySource") String categorySource);

    /** update() 경로(재동기화)는 PK 를 돌려주지 않는다. 멱등키로 다시 찾는다. */
    Long findIdByCodefTrKey(@Param("codefTrKey") String codefTrKey);

    /**
     * 규칙 기반 카테고리화 대상 조회. classification='CONSUMPTION', category_source 가 아직 NULL
     * (USER 는 물론 RULE_MCC·RULE_KEYWORD·LLM 으로도 아직 분류 안 된 거래만),
     * linked_transaction_id 가 있는 BANK(체크카드 승인과 중복) 제외까지 SQL 에서 미리 거른다.
     * 이미 분류된 거래를 재선택하면 재동기화마다 ruleCategorizedCount 가 실제로 새로 분류한 게
     * 없는데도 부풀려진다(이슈 #147 리뷰에서 발견).
     */
    List<Transaction> findEligibleForRuleCategorization(@Param("userId") long userId,
                                                         @Param("ids") List<Long> ids);

    /** id 목록으로 거래를 조회한다. LLM 배치 등록 시 transaction_date 정렬에 쓴다. */
    List<Transaction> findByIds(@Param("ids") List<Long> ids);

    /** 소유권 확인 겸 가맹점명 조회. 사용자 카테고리 수정 API(TransactionService) 전용. */
    Transaction findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 사용자가 직접 지정하는 카테고리 수정 전용. updateCategory()의 category_source 가드를 타지
     * 않는다 — 이미 USER로 지정된 거래를 다시 고치는 요청도 반영해야 하기 때문이다. WHERE에
     * user_id를 넣어 소유권을 DB 레벨에서도 강제한다.
     */
    int updateCategoryByUser(@Param("id") Long id, @Param("userId") Long userId,
                              @Param("categoryId") Long categoryId);
}

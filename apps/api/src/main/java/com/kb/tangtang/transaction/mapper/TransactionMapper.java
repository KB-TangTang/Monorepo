package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}

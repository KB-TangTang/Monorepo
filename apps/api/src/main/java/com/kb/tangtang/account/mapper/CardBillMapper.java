package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.CardBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardBillMapper {
    List<CardBill> findByCard(@Param("cardId") long cardId);

    /** UNIQUE(card_id, billing_month) 갱신 시도. 0행이면 신규 청구서 → insert 로 이어간다. */
    int update(CardBill cardBill);

    int insert(CardBill cardBill);
}

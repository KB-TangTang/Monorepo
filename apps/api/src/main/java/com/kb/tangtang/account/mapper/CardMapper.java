package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.Card;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardMapper {
    List<Card> findByUser(@Param("userId") long userId);

    /** UNIQUE(user_id, card_no_masked) 갱신 시도. 0행이면 신규 카드 → insert 로 이어간다. */
    int update(Card card);

    int insert(Card card);
}

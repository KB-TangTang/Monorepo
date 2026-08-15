package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.InvestmentHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvestmentHoldingMapper {
    List<InvestmentHolding> findByAccount(@Param("accountId") long accountId);

    /** 자산 상세(투자) 화면용 — 사용자의 전체 보유 종목. tbl_investment_holding 은 user_id 를 직접 갖고 있어 조인이 필요 없다. */
    List<InvestmentHolding> findByUser(@Param("userId") long userId);

    /** UNIQUE(account_id, symbol) 갱신 시도. 0행이면 신규 보유종목 → insert 로 이어간다. */
    int update(InvestmentHolding investmentHolding);

    int insert(InvestmentHolding investmentHolding);
}

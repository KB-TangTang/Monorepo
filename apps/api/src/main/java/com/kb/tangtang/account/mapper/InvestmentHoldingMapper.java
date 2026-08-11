package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.InvestmentHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvestmentHoldingMapper {
    List<InvestmentHolding> findByAccount(@Param("accountId") long accountId);

    /** UNIQUE(account_id, symbol) 갱신 시도. 0행이면 신규 보유종목 → insert 로 이어간다. */
    int update(InvestmentHolding investmentHolding);

    int insert(InvestmentHolding investmentHolding);
}

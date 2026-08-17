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

    /**
     * UNIQUE(account_id, symbol) 갱신 시도. 0행이면 신규 보유종목 → insert 로 이어간다.
     *
     * ⚠ 가격 관련 컬럼(last_price·market_value·profit_loss_amount·profit_loss_rate)은 건드리지
     *   않는다 — 그 네 컬럼은 이제 {@link #updatePrice} 를 통해 토스 실시간 시세가 소유한다.
     *   여기서 같이 덮으면 30분 배치가 돌 때마다 목서버의 가짜 가격이 실시간 가격을 지워버린다.
     */
    int updatePosition(InvestmentHolding investmentHolding);

    /**
     * 가격 전용 갱신. id 로 한 행만 정확히 짚는다 — 같은 심볼이라도 계좌·사용자마다 수량·매입금액이
     * 달라 market_value·profit_loss 는 행마다 따로 계산해서 넣어야 한다(InvestmentPriceRefresher 참고).
     */
    int updatePrice(InvestmentHolding investmentHolding);

    int insert(InvestmentHolding investmentHolding);
}

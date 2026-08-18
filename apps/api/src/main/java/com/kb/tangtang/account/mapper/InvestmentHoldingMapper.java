package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.InvestmentHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvestmentHoldingMapper {
    List<InvestmentHolding> findByAccount(@Param("accountId") long accountId);

    /**
     * 연결 상태와 무관하게 사용자의 전체 보유 종목(연결 해제한 계좌 포함). tbl_investment_holding 은
     * user_id 를 직접 갖고 있어 조인이 필요 없다.
     *
     * ⚠ 지금 프로덕션 호출부가 없다(자산 상세·순자산 계산 모두 {@link #findActiveByUser} 로 옮겼다,
     *   QA 지적사항). **새로 화면·계산을 붙일 때 이름만 보고 이 메서드를 고르지 말 것** — 대부분의
     *   경우 연결 해제한 계좌를 빼야 하는 {@link #findActiveByUser} 가 맞다. 여기 남겨둔 이유는
     *   "필터 없는 전체 조회" 자체를 검증하는 테스트(InvestmentHoldingMapperTest) 때문이다.
     */
    List<InvestmentHolding> findByUser(@Param("userId") long userId);

    /**
     * 순자산 계산(AssetCompositionCalculator)용 — 연결 해제(is_active=0)한 계좌의 보유종목은 뺀다
     * (QA 지적사항). AssetSummaryMapper#findAssetGroupsByUser 가 활성 계좌만 합산하는 것과 같은
     * 기준을 여기서도 지켜야 한다 — 안 그러면 이미 해제한 계좌의 종목까지 토스 시세를 갱신하게 되고,
     * 그 결과는 어차피 합계에서 빠지니 낭비다.
     */
    List<InvestmentHolding> findActiveByUser(@Param("userId") long userId);

    /**
     * UNIQUE(account_id, symbol) 갱신 시도. 0행이면 신규 보유종목 → insert 로 이어간다.
     *
     * ⚠ `last_price` 는 절대 SET 하지 않는다 — 그 컬럼은 {@link #updatePrice} 를 통해 토스
     *   실시간 시세만 쓴다. 여기서 같이 덮으면 30분 배치가 돌 때마다 목서버의 가짜 가격이
     *   실시간 가격을 지워버린다.
     *
     * ⚠ `market_value`·`profit_loss_amount`·`profit_loss_rate` 는 **여기서도 재계산한다** —
     *   DB 에 이미 있는 `last_price` 에 방금 반영된 수량을 곱해서 쓴다(토스를 다시 부르지 않는다).
     *   그러지 않으면 전량매도(quantity=0)한 뒤에도 평가금액이 옛날 값 그대로 남는다.
     */
    int updatePosition(InvestmentHolding investmentHolding);

    /**
     * 가격 전용 갱신. id 로 한 행만 정확히 짚는다 — 같은 심볼이라도 계좌·사용자마다 수량·매입금액이
     * 달라 market_value·profit_loss 는 행마다 따로 계산해서 넣어야 한다(InvestmentPriceRefresher 참고).
     */
    int updatePrice(InvestmentHolding investmentHolding);

    int insert(InvestmentHolding investmentHolding);
}

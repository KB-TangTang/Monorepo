package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.dto.AssetInvestmentDetailDto;
import com.kb.tangtang.account.dto.AssetInvestmentHoldingDto;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 자산 상세(투자) 조회.
 *
 * ⚠ findActiveByUser 를 쓴다(QA 지적사항) — 연결 해제(is_active=0)한 계좌의 보유종목은 뺀다.
 *   AssetCompositionCalculator(총자산 계산)가 이미 같은 기준으로 걸러내므로, 여기서 안 걸러내면
 *   자산 홈과 투자 상세 화면이 같은 사용자에 대해 다른 총액을 보여주고, 해제한 계좌의 종목까지
 *   매번 토스 시세를 갱신하는 낭비가 생긴다. (예전엔 tbl_investment_holding 이 user_id 를 직접
 *   가져서 조인 없이 findByUser 로 바로 조회했는데, 이제는 그 필터를 위해 조인이 필요하다.)
 *
 * ⚠ 조회하면서 오래된 심볼의 시세를 토스로 갱신하고 DB 에도 반영한다({@link InvestmentPriceRefresher}
 *   참고). **일부러 @Transactional 을 안 둔다**(QA 지적사항) — refresh() 가 토스에 블로킹 HTTP
 *   호출을 할 수 있는데, 트랜잭션을 걸면 그 호출이 끝날 때까지 DB 커넥션을 붙잡아 동시 요청이
 *   몰릴 때 커넥션 풀을 고갈시킬 수 있다. 아래 조회·갱신은 각자 독립된 단건 작업이라 하나의
 *   트랜잭션으로 묶일 필요가 없다.
 */
@Service
public class AssetInvestmentDetailService {

    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final InvestmentPriceRefresher investmentPriceRefresher;
    private final Clock clock;

    @Autowired
    public AssetInvestmentDetailService(InvestmentHoldingMapper investmentHoldingMapper,
                                        InvestmentPriceRefresher investmentPriceRefresher) {
        this(investmentHoldingMapper, investmentPriceRefresher, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AssetInvestmentDetailService(InvestmentHoldingMapper investmentHoldingMapper,
                                 InvestmentPriceRefresher investmentPriceRefresher, Clock clock) {
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.investmentPriceRefresher = investmentPriceRefresher;
        this.clock = clock;
    }

    public AssetInvestmentDetailDto getInvestments(long userId) {
        List<InvestmentHolding> holdings = investmentHoldingMapper.findActiveByUser(userId);
        investmentPriceRefresher.refresh(holdings);

        BigDecimal totalValuation = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<AssetInvestmentHoldingDto> holdingDtos = new ArrayList<>();
        for (InvestmentHolding holding : holdings) {
            totalValuation = totalValuation.add(holding.getMarketValue());
            totalCost = totalCost.add(holding.getPurchaseAmount());
            holdingDtos.add(AssetInvestmentHoldingDto.builder()
                    .accountId(holding.getAccountId())
                    .symbol(holding.getSymbol())
                    .name(holding.getName())
                    .marketCountry(holding.getMarketCountry())
                    .currency(holding.getCurrency())
                    .quantity(holding.getQuantity())
                    .averagePurchasePrice(holding.getAveragePurchasePrice())
                    .lastPrice(holding.getLastPrice())
                    .purchaseAmount(holding.getPurchaseAmount())
                    .marketValue(holding.getMarketValue())
                    .profitLossAmount(holding.getProfitLossAmount())
                    .profitLossRate(holding.getProfitLossRate())
                    .build());
        }

        return AssetInvestmentDetailDto.builder()
                .totalValuation(totalValuation)
                .totalCost(totalCost)
                .asOf(LocalDateTime.now(clock))
                .holdings(holdingDtos)
                .build();
    }
}

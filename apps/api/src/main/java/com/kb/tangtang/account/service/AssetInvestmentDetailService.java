package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.dto.AssetInvestmentDetailDto;
import com.kb.tangtang.account.dto.AssetInvestmentHoldingDto;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 자산 상세(투자) 조회. tbl_investment_holding 은 user_id 를 직접 갖고 있어
 * 연결계좌를 거치지 않고 바로 사용자 소유 보유종목만 조회한다.
 *
 * ⚠ 조회하면서 오래된 심볼의 시세를 토스로 갱신하고 DB 에도 반영한다({@link InvestmentPriceRefresher}
 *   참고) — 그래서 readOnly 트랜잭션이 아니다. readOnly = true 로 두면 이 안에서 일어나는 UPDATE 가
 *   드라이버·설정에 따라 조용히 무시되거나 예외로 막힐 수 있다.
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

    @Transactional
    public AssetInvestmentDetailDto getInvestments(long userId) {
        List<InvestmentHolding> holdings = investmentHoldingMapper.findByUser(userId);
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

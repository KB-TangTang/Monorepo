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
 */
@Service
public class AssetInvestmentDetailService {

    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final Clock clock;

    @Autowired
    public AssetInvestmentDetailService(InvestmentHoldingMapper investmentHoldingMapper) {
        this(investmentHoldingMapper, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AssetInvestmentDetailService(InvestmentHoldingMapper investmentHoldingMapper, Clock clock) {
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssetInvestmentDetailDto getInvestments(long userId) {
        List<InvestmentHolding> holdings = investmentHoldingMapper.findByUser(userId);

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

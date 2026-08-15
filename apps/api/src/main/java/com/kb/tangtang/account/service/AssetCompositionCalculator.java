package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자의 현재(라이브) 순자산·구성을 계산한다. {@link AssetSummaryService}(요약 카드)와
 * {@link AssetTrendService}(추이 그래프의 최신 달)가 공통으로 쓴다.
 *
 * 순자산 = 입출금 + 예적금 + 투자(SECURITIES 는 tbl_investment_holding.market_value 합계, report 모듈의
 * sumActiveTotalAssets 와 동일 규칙) + 페이머니 - 대출잔액. 연결 해제(is_active=0) 계좌는 제외한다.
 */
@Service
public class AssetCompositionCalculator {

    private static final List<String> ASSET_ACCOUNT_TYPES =
            List.of("DEMAND_DEPOSIT", "SAVINGS", "SECURITIES", "PAY_MONEY");
    private static final String LOAN_TYPE = "LOAN";

    /**
     * tbl_connected_account.account_type 의 실제 저장값은 "PAYMONEY"(밑줄 없음, FinancialSyncServiceImpl
     * 참고)다. DEMAND_DEPOSIT·SAVINGS·SECURITIES 와 달리 유일하게 이 API 계약의 밑줄 표기(PAY_MONEY)와
     * 어긋난다 — 계약을 DB 저장값에 맞춰 깨뜨리는 대신, 이 경계에서만 정규화한다.
     */
    private static final Map<String, String> DB_ACCOUNT_TYPE_ALIASES = Map.of("PAYMONEY", "PAY_MONEY");

    private static final Map<String, String> COMPOSITION_LABELS = Map.of(
            "DEMAND_DEPOSIT", "입출금",
            "SAVINGS", "예적금",
            "SECURITIES", "투자",
            "PAY_MONEY", "페이머니",
            "LOAN", "대출"
    );

    private static final Map<String, String> GROUP_LABELS = Map.of(
            "DEMAND_DEPOSIT", "입출금 계좌",
            "SAVINGS", "예금·적금",
            "SECURITIES", "투자·증권",
            "PAY_MONEY", "페이머니",
            "LOAN", "대출"
    );

    private final AssetSummaryMapper assetSummaryMapper;
    private final LoanMapper loanMapper;

    @Autowired
    public AssetCompositionCalculator(AssetSummaryMapper assetSummaryMapper, LoanMapper loanMapper) {
        this.assetSummaryMapper = assetSummaryMapper;
        this.loanMapper = loanMapper;
    }

    public AssetLiveComposition compute(long userId) {
        Map<String, AssetGroupRow> groupsByType = new LinkedHashMap<>();
        for (AssetGroupRow row : assetSummaryMapper.findAssetGroupsByUser(userId)) {
            String normalizedType = DB_ACCOUNT_TYPE_ALIASES.getOrDefault(row.getAccountType(), row.getAccountType());
            groupsByType.put(normalizedType, row);
        }
        BigDecimal loanBalance = zeroIfNull(loanMapper.sumBalanceByUser(userId));
        int loanCount = loanMapper.countByUser(userId);

        List<AssetCompositionItemDto> composition = new ArrayList<>();
        List<AssetGroupItemDto> assetGroups = new ArrayList<>();
        BigDecimal totalAsset = BigDecimal.ZERO;

        for (String type : ASSET_ACCOUNT_TYPES) {
            AssetGroupRow row = groupsByType.get(type);
            BigDecimal amount = row == null ? BigDecimal.ZERO : zeroIfNull(row.getAmount());
            int count = row == null ? 0 : row.getCount();
            totalAsset = totalAsset.add(amount);
            composition.add(AssetCompositionItemDto.builder()
                    .type(type).label(COMPOSITION_LABELS.get(type)).amount(amount).build());
            assetGroups.add(AssetGroupItemDto.builder()
                    .type(type).label(GROUP_LABELS.get(type)).count(count).amount(amount).build());
        }

        BigDecimal negativeLoanBalance = loanBalance.negate();
        composition.add(AssetCompositionItemDto.builder()
                .type(LOAN_TYPE).label(COMPOSITION_LABELS.get(LOAN_TYPE)).amount(negativeLoanBalance).build());
        assetGroups.add(AssetGroupItemDto.builder()
                .type(LOAN_TYPE).label(GROUP_LABELS.get(LOAN_TYPE)).count(loanCount).amount(negativeLoanBalance).build());

        BigDecimal netWorth = totalAsset.subtract(loanBalance);

        return AssetLiveComposition.builder()
                .netWorth(netWorth)
                .totalDebt(loanBalance)
                .composition(composition)
                .assetGroups(assetGroups)
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}

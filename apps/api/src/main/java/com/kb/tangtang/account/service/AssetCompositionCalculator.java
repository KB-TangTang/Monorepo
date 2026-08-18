package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import com.kb.tangtang.account.mapper.InvestmentHoldingMapper;
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
 *
 * ⚠ SECURITIES 합계를 구하기 전에 {@link InvestmentPriceRefresher} 로 오래된 심볼만 먼저 갱신한다
 *   (QA 지적사항) — 그러지 않으면 목서버 동기화가 더 이상 market_value 를 건드리지 않는 지금 구조에서,
 *   투자 상세 화면(/asset/investment)을 한 번도 안 연 사용자는 총자산이 영원히 갱신되지 않는다.
 *   자산 홈(이 클래스 호출부)이 투자 상세보다 훨씬 자주 열리는 화면이라 여기서도 갱신하면
 *   실질적인 신선도가 크게 개선된다. 월간 리포트 배치(MonthlyReportBatchScheduler)에는 일부러
 *   연결하지 않았다 — 그건 특정 사용자의 조회가 아니라 전체 사용자를 순회하는 배치라서, 여기에
 *   연결하면 접속 안 하는 사용자 몫까지 토스를 부르게 되는, 애초에 피하려던 문제로 되돌아간다.
 *
 * ⚠ 갱신 대상은 {@code findByUser} 가 아니라 {@code findActiveByUser} 다(QA 지적사항) — 아래
 *   findAssetGroupsByUser 자체가 연결 해제(is_active=0)한 계좌를 이미 빼고 합산하므로, 해제한
 *   계좌의 종목까지 토스로 갱신하는 건 결과에 반영되지도 않는 순수 낭비다.
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
    private final InvestmentHoldingMapper investmentHoldingMapper;
    private final InvestmentPriceRefresher investmentPriceRefresher;

    @Autowired
    public AssetCompositionCalculator(AssetSummaryMapper assetSummaryMapper, LoanMapper loanMapper,
                                      InvestmentHoldingMapper investmentHoldingMapper,
                                      InvestmentPriceRefresher investmentPriceRefresher) {
        this.assetSummaryMapper = assetSummaryMapper;
        this.loanMapper = loanMapper;
        this.investmentHoldingMapper = investmentHoldingMapper;
        this.investmentPriceRefresher = investmentPriceRefresher;
    }

    public AssetLiveComposition compute(long userId) {
        List<InvestmentHolding> holdings = investmentHoldingMapper.findActiveByUser(userId);
        investmentPriceRefresher.refresh(holdings);

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

package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetLiveComposition;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetCompositionCalculatorTest {

    private static final long USER_ID = 1L;

    @Mock
    private AssetSummaryMapper assetSummaryMapper;

    @Mock
    private LoanMapper loanMapper;

    private AssetCompositionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AssetCompositionCalculator(assetSummaryMapper, loanMapper);
    }

    @Test
    @DisplayName("계좌·대출 잔액으로 순자산과 구성·자산목록을 계산한다")
    void computesLiveNetWorthAndComposition() {
        // ⚠ 페이머니는 DB 저장값이 "PAYMONEY"(밑줄 없음, FinancialSyncServiceImpl 참고)다 —
        // API 계약("PAY_MONEY")과 다르다는 걸 실제로 검증하려고 일부러 원본 DB 값을 그대로 목킹한다.
        when(assetSummaryMapper.findAssetGroupsByUser(USER_ID)).thenReturn(List.of(
                new AssetGroupRow("DEMAND_DEPOSIT", 2, new BigDecimal("2066800")),
                new AssetGroupRow("SAVINGS", 2, new BigDecimal("5420000")),
                new AssetGroupRow("SECURITIES", 1, new BigDecimal("3214200")),
                new AssetGroupRow("PAYMONEY", 1, new BigDecimal("244500"))
        ));
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(new BigDecimal("1500000"));
        when(loanMapper.countByUser(USER_ID)).thenReturn(1);

        AssetLiveComposition result = calculator.compute(USER_ID);

        // 페이머니(244,500)가 정상적으로 순자산에 합산돼야 9,445,500 이다.
        // DB 값 "PAYMONEY" 를 "PAY_MONEY" 로 정규화하지 못하면 이 항목이 통째로 빠져 9,201,000 이 된다.
        assertEquals(new BigDecimal("9445500"), result.getNetWorth());
        assertEquals(new BigDecimal("1500000"), result.getTotalDebt());

        Map<String, BigDecimal> compositionByType = result.getComposition().stream()
                .collect(Collectors.toMap(AssetCompositionItemDto::getType, AssetCompositionItemDto::getAmount));
        assertEquals(new BigDecimal("2066800"), compositionByType.get("DEMAND_DEPOSIT"));
        assertEquals(new BigDecimal("3214200"), compositionByType.get("SECURITIES"));
        // API 계약의 키는 여전히 "PAY_MONEY"(밑줄 포함)다 — DB 저장값과 무관하게 계약은 그대로 유지된다.
        assertEquals(new BigDecimal("244500"), compositionByType.get("PAY_MONEY"));
        assertEquals(new BigDecimal("-1500000"), compositionByType.get("LOAN"));

        AssetGroupItemDto payMoneyGroup = result.getAssetGroups().stream()
                .filter(g -> g.getType().equals("PAY_MONEY")).findFirst().orElseThrow();
        assertEquals(1, payMoneyGroup.getCount());
        assertEquals(new BigDecimal("244500"), payMoneyGroup.getAmount());

        AssetGroupItemDto loanGroup = result.getAssetGroups().stream()
                .filter(g -> g.getType().equals("LOAN")).findFirst().orElseThrow();
        assertEquals(1, loanGroup.getCount());
        assertEquals(new BigDecimal("-1500000"), loanGroup.getAmount());
    }

    @Test
    @DisplayName("연동된 계좌가 없는 종류는 개수 0·금액 0으로 채운다")
    void fillsMissingAccountTypesWithZero() {
        when(assetSummaryMapper.findAssetGroupsByUser(USER_ID)).thenReturn(List.of());
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(loanMapper.countByUser(USER_ID)).thenReturn(0);

        AssetLiveComposition result = calculator.compute(USER_ID);

        assertEquals(new BigDecimal("0"), result.getNetWorth());
        assertEquals(new BigDecimal("0"), result.getTotalDebt());
        assertEquals(5, result.getComposition().size());
        assertEquals(5, result.getAssetGroups().size());
        assertEquals(0, result.getAssetGroups().get(0).getCount());
    }
}

package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetSnapshotRow;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetSummaryServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Mock
    private AssetSummaryMapper assetSummaryMapper;

    @Mock
    private LoanMapper loanMapper;

    private AssetSummaryService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(TODAY);
    }

    private AssetSummaryService serviceAt(LocalDate currentDate) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(currentDate.atTime(10, 30).atZone(zoneId).toInstant(), zoneId);
        return new AssetSummaryService(assetSummaryMapper, loanMapper, clock);
    }

    private void stubEmptySnapshots() {
        when(assetSummaryMapper.findNetWorthSnapshots(eq(USER_ID), anyList())).thenReturn(List.of());
    }

    @Test
    @DisplayName("계좌·대출 잔액으로 순자산과 구성·자산목록을 계산한다")
    void returnsAssetSummary() {
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
        stubEmptySnapshots();

        AssetSummaryDto result = service.getSummary(USER_ID, null);

        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 30), result.getAsOf());
        // 페이머니(244,500)가 정상적으로 순자산에 합산돼야 9,445,500 이다.
        // DB 값 "PAYMONEY" 를 "PAY_MONEY" 로 정규화하지 못하면 이 항목이 통째로 빠져 9,201,000 이 된다.
        assertEquals(new BigDecimal("9445500"), result.getNetWorth());
        assertNull(result.getPreviousMonthNetWorth());
        assertNull(result.getChangeAmount());
        assertNull(result.getChangeRate());

        Map<String, BigDecimal> compositionByType = result.getComposition().stream()
                .collect(Collectors.toMap(
                        AssetCompositionItemDto::getType, AssetCompositionItemDto::getAmount));
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

        AssetTrendItemDto lastTrendPoint = result.getTrend().get(result.getTrend().size() - 1);
        assertEquals("2026-08", lastTrendPoint.getYearMonth());
        assertEquals(new BigDecimal("9445500"), lastTrendPoint.getNetWorth());
        assertEquals(new BigDecimal("1500000"), lastTrendPoint.getTotalDebt());
        assertEquals(6, result.getTrend().size());
    }

    @Test
    @DisplayName("연동된 계좌가 없는 종류는 개수 0·금액 0으로 채운다")
    void fillsMissingAccountTypesWithZero() {
        when(assetSummaryMapper.findAssetGroupsByUser(USER_ID)).thenReturn(List.of());
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(loanMapper.countByUser(USER_ID)).thenReturn(0);
        stubEmptySnapshots();

        AssetSummaryDto result = service.getSummary(USER_ID, null);

        assertEquals(new BigDecimal("0"), result.getNetWorth());
        assertEquals(5, result.getComposition().size());
        assertEquals(5, result.getAssetGroups().size());
        assertEquals(0, result.getAssetGroups().get(0).getCount());
    }

    @Test
    @DisplayName("전월 스냅샷이 있으면 증감액·증감률을 계산한다")
    void calculatesChangeWhenPreviousSnapshotExists() {
        when(assetSummaryMapper.findAssetGroupsByUser(USER_ID)).thenReturn(List.of(
                new AssetGroupRow("DEMAND_DEPOSIT", 1, new BigDecimal("9445500"))
        ));
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(loanMapper.countByUser(USER_ID)).thenReturn(0);
        when(assetSummaryMapper.findNetWorthSnapshots(eq(USER_ID), anyList())).thenReturn(List.of(
                new AssetSnapshotRow("2026-07", new BigDecimal("9125500"), new BigDecimal("1600000"))
        ));

        AssetSummaryDto result = service.getSummary(USER_ID, "2026-08-15");

        assertEquals(new BigDecimal("9125500"), result.getPreviousMonthNetWorth());
        assertEquals(new BigDecimal("320000"), result.getChangeAmount());
        assertEquals(new BigDecimal("3.51"), result.getChangeRate());

        AssetTrendItemDto julyPoint = result.getTrend().stream()
                .filter(item -> item.getYearMonth().equals("2026-07")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1600000"), julyPoint.getTotalDebt());
    }

    @Test
    @DisplayName("baseDate 형식이 잘못되면 INVALID_REQUEST 예외를 던진다")
    void throwsOnInvalidBaseDate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026/08/15"));

        assertEquals("INVALID_REQUEST", ex.getCode());
    }
}

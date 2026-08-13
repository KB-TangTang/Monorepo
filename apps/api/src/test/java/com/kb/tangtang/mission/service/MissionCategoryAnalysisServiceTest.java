package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.CategorySpending;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.mapper.MissionCategoryAnalysisMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionCategoryAnalysisServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 11);

    private static class FakeMapper implements MissionCategoryAnalysisMapper {
        int allTransactionCount;
        int transactionCount;
        BigDecimal totalConsumption = BigDecimal.ZERO;
        List<CategorySpending> topCategories = List.of();
        int sumConsumptionCallCount;
        int findTopCategoriesCallCount;

        @Override
        public int countAllConsumptionTransactions(long userId) {
            return allTransactionCount;
        }

        @Override
        public int countConsumptionTransactions(long userId, LocalDate startDate, LocalDate endDate) {
            assertEquals(LocalDate.of(2026, 7, 14), startDate);
            assertEquals(TODAY, endDate);
            return transactionCount;
        }

        @Override
        public BigDecimal sumCategorizedConsumption(long userId, LocalDate startDate, LocalDate endDate) {
            sumConsumptionCallCount++;
            return totalConsumption;
        }

        @Override
        public List<CategorySpending> findTopCategorySpending(long userId, LocalDate startDate,
                                                              LocalDate endDate, int limit) {
            findTopCategoriesCallCount++;
            assertEquals(3, limit);
            return topCategories;
        }
    }

    private Clock fixedClock() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        return Clock.fixed(TODAY.atStartOfDay(zoneId).toInstant(), zoneId);
    }

    private MissionCategoryAnalysisService service(FakeMapper mapper) {
        return new MissionCategoryAnalysisService(mapper, fixedClock());
    }

    @Test
    @DisplayName("최근 거래가 많아도 누적 유효 소비가 50건 미만이면 분석을 생략한다")
    void skipsAnalysisWhenCumulativeTransactionsAreInsufficient() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 49;
        mapper.transactionCount = 70;

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertFalse(result.isRelativeEligible());
        assertTrue(result.getTopCategories().isEmpty());
        assertEquals(0, mapper.sumConsumptionCallCount);
        assertEquals(0, mapper.findTopCategoriesCallCount);
    }

    @Test
    @DisplayName("최근 28일 소비 거래가 50건 미만이면 상위 카테고리 집계를 생략한다")
    void skipsAnalysisWhenTransactionCountIsInsufficient() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 49;
        mapper.transactionCount = 49;

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertFalse(result.isRelativeEligible());
        assertEquals(49, result.getTransactionCount());
        assertTrue(result.getTopCategories().isEmpty());
        assertEquals(0, mapper.sumConsumptionCallCount);
        assertEquals(0, mapper.findTopCategoriesCallCount);
    }

    @Test
    @DisplayName("최초 자격 획득 사용자는 최근 28일 거래가 50건 미만이어도 분석한다")
    void qualifiedUserAnalyzesWithFewerThanFiftyTransactions() {
        FakeMapper mapper = new FakeMapper();
        mapper.transactionCount = 12;
        mapper.totalConsumption = new BigDecimal("300000");
        mapper.topCategories = List.of(row(17L, "식비", "배달앱", "120000", 4));

        MissionCategoryAnalysisDto result = service(mapper)
                .getCategoryAnalysisForQualifiedUser(USER_ID);

        assertTrue(result.isRelativeEligible());
        assertEquals(12, result.getTransactionCount());
        assertEquals("배달앱", result.getTopCategories().get(0).getCategoryName());
        assertEquals(1, mapper.findTopCategoriesCallCount);
    }

    @Test
    @DisplayName("누적 유효 소비 거래가 정확히 50건이면 상대형 분석을 실행한다")
    void analyzesWhenCumulativeTransactionRequirementIsExactlyMet() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 50;
        mapper.transactionCount = 50;
        mapper.totalConsumption = new BigDecimal("500000");
        mapper.topCategories = List.of(row(17L, "식비", "배달앱", "150000", 10));

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertTrue(result.isRelativeEligible());
        assertEquals(LocalDate.of(2026, 7, 14), result.getAnalysisStartDate());
        assertEquals(LocalDate.of(2026, 8, 10), result.getAnalysisEndDate());
        assertEquals("배달앱", result.getTopCategories().get(0).getCategoryName());
        assertEquals(1, mapper.sumConsumptionCallCount);
        assertEquals(1, mapper.findTopCategoriesCallCount);
    }

    @Test
    @DisplayName("누적 유효 소비 거래가 50건이면 상대형 분석을 실행한다")
    void analyzesWithFiftyCumulativeTransactions() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 50;
        mapper.transactionCount = 50;
        mapper.totalConsumption = new BigDecimal("500000");
        mapper.topCategories = List.of(row(18L, "식비", "카페/간식", "150000", 10));

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertTrue(result.isRelativeEligible());
        assertEquals(1, mapper.findTopCategoriesCallCount);
    }

    @Test
    @DisplayName("조회 순서대로 순위를 붙이고 전체 소비 대비 비중을 계산한다")
    void createsRankAndSpendingRatio() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 80;
        mapper.transactionCount = 80;
        mapper.totalConsumption = new BigDecimal("500000");
        mapper.topCategories = List.of(
                row(17L, "식비", "배달앱", "150000", 10),
                row(16L, "식비", "음식점/외식", "100000", 6),
                row(18L, "식비", "카페/간식", "50000", 8));

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertEquals(3, result.getTopCategories().size());
        assertEquals(1, result.getTopCategories().get(0).getRank());
        assertEquals(new BigDecimal("30.00"), result.getTopCategories().get(0).getSpendingRatio());
        assertEquals(2, result.getTopCategories().get(1).getRank());
        assertEquals(new BigDecimal("20.00"), result.getTopCategories().get(1).getSpendingRatio());
        assertEquals(3, result.getTopCategories().get(2).getRank());
        assertEquals(new BigDecimal("10.00"), result.getTopCategories().get(2).getSpendingRatio());
    }

    @Test
    @DisplayName("15개 미션 대상에 양수 소비가 없으면 상대형 불가로 반환한다")
    void returnsIneligibleWhenNoMissionCategorySpendingExists() {
        FakeMapper mapper = new FakeMapper();
        mapper.allTransactionCount = 60;
        mapper.transactionCount = 60;
        mapper.topCategories = List.of();

        MissionCategoryAnalysisDto result = service(mapper).getCategoryAnalysis(USER_ID);

        assertFalse(result.isRelativeEligible());
        assertTrue(result.getTopCategories().isEmpty());
        assertEquals(1, mapper.findTopCategoriesCallCount);
        assertEquals(0, mapper.sumConsumptionCallCount);
    }

    private CategorySpending row(long categoryId, String parentCategoryName, String categoryName,
                                 String totalAmount, int transactionCount) {
        return new CategorySpending(categoryId, parentCategoryName, categoryName,
                new BigDecimal(totalAmount), transactionCount);
    }
}

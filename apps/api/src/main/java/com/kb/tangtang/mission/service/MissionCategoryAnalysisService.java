package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.CategorySpending;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.dto.MissionCategoryRankDto;
import com.kb.tangtang.mission.mapper.MissionCategoryAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MissionCategoryAnalysisService {

    private static final int ANALYSIS_DAYS = 28;
    private static final int MIN_TRANSACTION_COUNT = 50;
    private static final int TOP_CATEGORY_LIMIT = 3;

    private final MissionCategoryAnalysisMapper missionCategoryAnalysisMapper;
    private final Clock clock;

    @Autowired
    public MissionCategoryAnalysisService(MissionCategoryAnalysisMapper missionCategoryAnalysisMapper) {
        this(missionCategoryAnalysisMapper, Clock.systemDefaultZone());
    }

    MissionCategoryAnalysisService(MissionCategoryAnalysisMapper missionCategoryAnalysisMapper, Clock clock) {
        this.missionCategoryAnalysisMapper = missionCategoryAnalysisMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MissionCategoryAnalysisDto getCategoryAnalysis(long userId) {
        return getCategoryAnalysis(userId, true);
    }

    @Transactional(readOnly = true)
    public MissionCategoryAnalysisDto getCategoryAnalysisForQualifiedUser(long userId) {
        return getCategoryAnalysis(userId, false);
    }

    private MissionCategoryAnalysisDto getCategoryAnalysis(long userId, boolean requireInitialQualification) {
        LocalDate endDateExclusive = LocalDate.now(clock);
        LocalDate startDate = endDateExclusive.minusDays(ANALYSIS_DAYS);
        LocalDate analysisEndDate = endDateExclusive.minusDays(1);

        int allTransactionCount = missionCategoryAnalysisMapper.countAllConsumptionTransactions(userId);
        int transactionCount = missionCategoryAnalysisMapper.countConsumptionTransactions(
                userId, startDate, endDateExclusive);

        boolean requirementsMet = allTransactionCount >= MIN_TRANSACTION_COUNT;

        if (requireInitialQualification && !requirementsMet) {
            return result(startDate, analysisEndDate, transactionCount, false, List.of());
        }

        List<CategorySpending> spendingRows = missionCategoryAnalysisMapper.findTopCategorySpending(
                userId, startDate, endDateExclusive, TOP_CATEGORY_LIMIT);
        if (spendingRows.isEmpty()) {
            return result(startDate, analysisEndDate, transactionCount, false, List.of());
        }

        BigDecimal totalConsumption = zeroIfNull(
                missionCategoryAnalysisMapper.sumCategorizedConsumption(
                        userId, startDate, endDateExclusive));
        List<MissionCategoryRankDto> topCategories = createTopCategories(
                userId, spendingRows, totalConsumption);

        return result(startDate, analysisEndDate, transactionCount, true, topCategories);
    }

    private MissionCategoryAnalysisDto result(LocalDate startDate,
                                              LocalDate endDate,
                                              int transactionCount,
                                              boolean relativeEligible,
                                              List<MissionCategoryRankDto> topCategories) {
        return MissionCategoryAnalysisDto.builder()
                .analysisStartDate(startDate)
                .analysisEndDate(endDate)
                .transactionCount(transactionCount)
                .relativeEligible(relativeEligible)
                .topCategories(topCategories)
                .build();
    }

    private List<MissionCategoryRankDto> createTopCategories(long userId,
                                                              List<CategorySpending> spendingRows,
                                                              BigDecimal totalConsumption) {
        List<MissionCategoryRankDto> topCategories = new ArrayList<>();
        for (int index = 0; index < spendingRows.size(); index++) {
            CategorySpending row = spendingRows.get(index);
            topCategories.add(MissionCategoryRankDto.builder()
                    .rank(index + 1)
                    .categoryId(row.getCategoryId())
                    .parentCategoryName(row.getParentCategoryName())
                    .categoryName(row.getCategoryName())
                    .totalAmount(row.getTotalAmount())
                    .transactionCount(row.getTransactionCount())
                    .spendingRatio(calculateSpendingRatio(row.getTotalAmount(), totalConsumption))
                    .build());
        }
        return topCategories;
    }

    private BigDecimal calculateSpendingRatio(BigDecimal categoryAmount, BigDecimal totalConsumption) {
        if (totalConsumption.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return zeroIfNull(categoryAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(totalConsumption, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}

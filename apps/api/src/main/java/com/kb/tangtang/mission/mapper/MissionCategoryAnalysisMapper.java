package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.CategorySpending;
import com.kb.tangtang.mission.domain.CategoryMissionStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MissionCategoryAnalysisMapper {

    int countAllConsumptionTransactions(@Param("userId") long userId);

    int countConsumptionTransactions(@Param("userId") long userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    BigDecimal sumCategorizedConsumption(@Param("userId") long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    List<CategorySpending> findTopCategorySpending(@Param("userId") long userId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("limit") int limit);

    CategoryMissionStatus findLatestMissionStatus(@Param("userId") long userId,
                                                  @Param("categoryId") long categoryId);
}

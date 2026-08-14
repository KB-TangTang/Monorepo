package com.kb.tangtang.fixedexpense.mapper;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionRule;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 월간 고정지출 탐지에 필요한 룰·거래·후보 접근. */
@Mapper
public interface FixedExpenseDetectionMapper {

    FixedExpenseDetectionRule findActiveRule();

    List<Long> findActiveUserIds();

    List<FixedExpenseDetectionTransaction> findDetectionTransactions(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    FixedExpenseCandidate findCandidate(@Param("userId") long userId,
                                        @Param("merchantNameNormalized") String merchantNameNormalized);

    int upsertCandidate(FixedExpenseCandidate candidate);

    int updateDetectedCandidate(@Param("candidate") FixedExpenseCandidate candidate,
                                 @Param("reactivateBuffer") boolean reactivateBuffer,
                                 @Param("relapseDetectedAt") LocalDateTime relapseDetectedAt);

    int linkTransactionsToCandidate(@Param("userId") long userId,
                                    @Param("merchantNameNormalized") String merchantNameNormalized,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("candidateId") long candidateId);
}

package com.kb.tangtang.fixedexpense.mapper;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseDetectionTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 확정 고정지출의 결제 검증 수명주기 전용 접근 계층. */
@Mapper
public interface FixedExpenseLifecycleMapper {

    List<FixedExpenseCandidate> findLifecycleCandidates(@Param("userId") long userId);

    List<FixedExpenseDetectionTransaction> findEligiblePayments(
            @Param("userId") long userId,
            @Param("merchantNameNormalized") String merchantNameNormalized,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int advanceConfirmedCandidate(@Param("candidateId") long candidateId,
                                  @Param("userId") long userId,
                                  @Param("expectedDate") LocalDate expectedDate,
                                  @Param("nextExpectedDate") LocalDate nextExpectedDate);

    int moveConfirmedCandidateToBuffer(@Param("candidateId") long candidateId,
                                       @Param("userId") long userId,
                                       @Param("expectedDate") LocalDate expectedDate,
                                       @Param("nextExpectedDate") LocalDate nextExpectedDate);

    int reactivateBufferedCandidate(@Param("candidateId") long candidateId,
                                    @Param("userId") long userId,
                                    @Param("expectedDate") LocalDate expectedDate,
                                    @Param("nextExpectedDate") LocalDate nextExpectedDate,
                                    @Param("relapseDetectedAt") LocalDateTime relapseDetectedAt);

    int verifyBufferedCandidate(@Param("candidateId") long candidateId,
                                @Param("userId") long userId,
                                @Param("expectedDate") LocalDate expectedDate,
                                @Param("verifiedAt") LocalDateTime verifiedAt);

    int linkPaymentToCandidate(@Param("transactionId") long transactionId,
                               @Param("userId") long userId,
                               @Param("candidateId") long candidateId);
}

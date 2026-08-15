package com.kb.tangtang.fixedexpense.mapper;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseReadRow;
import com.kb.tangtang.fixedexpense.dto.FixedExpensePaymentHistoryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 고정지출 관리·상세·절약 감정서의 읽기 전용 접근 계층이다. */
@Mapper
public interface FixedExpenseQueryMapper {

    List<FixedExpenseReadRow> findActiveItems(@Param("userId") long userId,
                                              @Param("categoryId") Long categoryId,
                                              @Param("confirmed") boolean confirmed);

    FixedExpenseReadRow findOwnedActiveItem(@Param("userId") long userId,
                                            @Param("candidateId") long candidateId);

    List<FixedExpensePaymentHistoryDto> findRecentPaymentHistory(
            @Param("userId") long userId,
            @Param("candidateId") long candidateId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    BigDecimal sumPaymentHistory(@Param("userId") long userId,
                                 @Param("candidateId") long candidateId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}

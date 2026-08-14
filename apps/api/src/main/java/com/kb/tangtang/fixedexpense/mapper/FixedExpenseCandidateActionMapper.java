package com.kb.tangtang.fixedexpense.mapper;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/** 고정지출 후보의 사용자 확정·제외 명령 전용 접근 계층. */
@Mapper
public interface FixedExpenseCandidateActionMapper {

    /**
     * 조건부 확정이 0건일 때 최신 상태를 판정한다.
     * 소유자 조건을 SQL에 포함해 없는 후보와 타인 후보를 구분하지 않는다.
     */
    FixedExpenseCandidate findOwnedCandidateForUpdate(@Param("candidateId") long candidateId,
                                                      @Param("userId") long userId);

    /** 활성·미제외·미확정 후보만 확정한다. */
    int confirmCandidate(@Param("candidateId") long candidateId,
                         @Param("userId") long userId,
                         @Param("confirmedAt") LocalDateTime confirmedAt);

    /** 활성·미제외·미확정 후보만 제외한다. */
    int excludeCandidate(@Param("candidateId") long candidateId,
                         @Param("userId") long userId);
}

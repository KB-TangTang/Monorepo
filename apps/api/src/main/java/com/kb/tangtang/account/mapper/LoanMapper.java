package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.Loan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface LoanMapper {
    List<Loan> findByUser(@Param("userId") long userId);

    /** UNIQUE(user_id, loan_no_encrypted) 갱신 시도. 0행이면 신규 대출 → insert 로 이어간다. */
    int update(Loan loan);

    int insert(Loan loan);

    /** 자산 현황 카드용 대출 잔액 합계. 행이 없으면 0 (report 모듈 sumLoanBalances 와 동일 규칙). */
    BigDecimal sumBalanceByUser(@Param("userId") long userId);

    /** 자산 현황 카드의 대출 그룹 개수 표기용. */
    int countByUser(@Param("userId") long userId);

    /**
     * 연결 해제(#467). tbl_loan 에는 is_active 가 없어 행을 지운다 —
     * tbl_transaction.loan_id 는 ON DELETE SET NULL 이라 거래는 남는다.
     * 다시 동기화돼도 되살아나지 않게 하는 건 AccountLinkService.disconnect() 가 남기는 제외 키다.
     */
    int deleteByIdAndUser(@Param("id") long id, @Param("userId") long userId);
}

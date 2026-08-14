package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.Loan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoanMapper {
    List<Loan> findByUser(@Param("userId") long userId);

    /** UNIQUE(user_id, loan_no_encrypted) 갱신 시도. 0행이면 신규 대출 → insert 로 이어간다. */
    int update(Loan loan);

    int insert(Loan loan);
}

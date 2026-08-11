package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.FinancialSyncHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FinancialSyncHistoryMapper {
    int insert(FinancialSyncHistory history);
}

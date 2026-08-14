package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.FinancialSyncResultDto;

public interface FinancialSyncService {
    FinancialSyncResultDto sync(long userId);
}

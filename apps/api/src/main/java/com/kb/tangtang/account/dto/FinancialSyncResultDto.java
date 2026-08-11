package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FinancialSyncResultDto {
    private String status;
    private List<String> syncedSources;
    private String syncedAt;
}

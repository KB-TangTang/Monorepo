package com.kb.tangtang.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 운영자가 실행하는 월간 소비 리포트 수동 배치 요청이다. */
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportManualBatchRequestDto {

    private String yearMonth;
    private boolean force;
    private List<Long> targetUserIds = new ArrayList<>();
    private List<MonthlyReportAiConsentOverrideDto> missingSnapshotAiConsents = new ArrayList<>();
}

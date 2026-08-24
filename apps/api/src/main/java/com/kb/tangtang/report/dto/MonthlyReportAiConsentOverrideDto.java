package com.kb.tangtang.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 이전 스냅샷이 없어 당시 AI 동의를 알 수 없는 사용자의 운영자 입력값이다. */
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportAiConsentOverrideDto {

    private Long userId;
    private Boolean aiUsageConsented;
}

package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 즉시 조회 결과.
 * 쿨다운은 서버가 판단해 내려준다 — 프론트가 직접 계산하지 않는다.
 */
@Getter
@Builder
public class RefreshResultDto {

    private final String lastSyncAt;
    private final int institutionCount;
    private final int cooldownSeconds;
    private final java.util.List<RefreshInstitutionDto> institutions;
}

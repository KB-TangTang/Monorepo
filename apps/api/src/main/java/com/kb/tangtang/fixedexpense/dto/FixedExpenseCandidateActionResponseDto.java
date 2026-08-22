package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 후보 확정·제외 후 프론트가 상태를 갱신하는 데 필요한 최소 정보. */
@Getter
@Builder
public class FixedExpenseCandidateActionResponseDto {

    private long candidateId;
    private String status;
    private Boolean isExcluded;
    private LocalDateTime confirmedAt;
}

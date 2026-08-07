package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 기관별 조회 진행 상황. 진행률은 서버가 계산해 내려준다(프론트 연출이 아니다).
 */
@Getter
@Builder
public class LinkProgressDto {

    private final int percent;
    private final boolean done;
    private final java.util.List<ProgressRowDto> institutions;
}

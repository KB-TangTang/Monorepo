package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 초대 코드 미리보기 (GC_01_05).
 *
 * 코드가 아예 없으면 404 대신 {@code BusinessException} 으로 끝내고, 코드는 유효하지만
 * 참여할 수 없는 상태(만료·정원 초과·중복 참여·종료)는 사유를 담아 200 으로 내려준다.
 * 참여 확인 화면(GC_01_06)이 그룹 정보를 보여준 뒤 사유를 안내해야 하기 때문이다.
 */
@Getter
@Builder
public class InviteCodePreviewDto {

    private ChallengeGroupDetailDto challenge;

    /** 지금 참여할 수 있는지. */
    private boolean joinable;

    /**
     * 참여 불가 사유. joinable=true 면 NULL.
     * EXPIRED / FULL / ALREADY_JOINED / CLOSED
     */
    private String reason;
}

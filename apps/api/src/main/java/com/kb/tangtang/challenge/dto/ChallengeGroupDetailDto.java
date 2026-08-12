package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 그룹 챌린지 상세.
 *
 * 목록 아이템에 메모와 "내가 참여자인가"를 더한 형태다.
 * 초대 코드 미리보기(GC_01_05)에서도 재사용한다 — 이때 {@code member} 는 false 다.
 */
@Getter
@Builder
public class ChallengeGroupDetailDto {

    private ChallengeGroupSummaryDto challenge;

    /** 리워드·벌칙 자유 메모. 표시 전용. */
    private String memo;

    /** 로그인 사용자가 이 그룹의 참여자인지. */
    private boolean member;

    /** 지금 참여할 수 있는지 (모집 중 · 정원 여유 · 미참여). */
    private boolean joinable;
}

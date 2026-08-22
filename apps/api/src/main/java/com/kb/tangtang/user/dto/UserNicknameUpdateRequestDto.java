package com.kb.tangtang.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PATCH /api/users/me/nickname 요청 본문.
 *
 * 온보딩(AU_03_01)과 마이페이지 수정(MY_01_03)이 같은 엔드포인트를 쓴다.
 * 표시명만 받는다 — 실명(name)은 별개 컬럼·별개 엔드포인트다.
 * (DECISIONS.md 2026-08-11 닉네임 온보딩)
 */
@Getter
@Setter
public class UserNicknameUpdateRequestDto {

    private String nickname;
}

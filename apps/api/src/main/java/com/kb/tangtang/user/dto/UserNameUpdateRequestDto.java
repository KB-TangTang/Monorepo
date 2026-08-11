package com.kb.tangtang.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PATCH /api/users/me/name 요청 본문.
 *
 * 실명(본인확인용)만 받는다. 닉네임(표시명)은 별개 컬럼·별개 엔드포인트다 —
 * 두 값을 한 요청으로 받으면 목적이 섞인다. (DECISIONS.md 2026-08-11 (4))
 */
@Getter
@Setter
public class UserNameUpdateRequestDto {

    private String name;
}

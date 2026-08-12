package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 그룹 참여자 표시용.
 *
 * 이니셜은 내려주지 않는다. {@code tbl_user} 에 없는 표시용 파생값이라 프론트가 닉네임에서 만든다.
 */
@Getter
@Builder
public class GroupMemberDto {

    private Long userId;

    /** {@code tbl_user.nickname} 은 NULL 허용이라 값이 없을 수 있다. */
    private String nickname;

    /** 방장 여부. */
    private boolean owner;
}

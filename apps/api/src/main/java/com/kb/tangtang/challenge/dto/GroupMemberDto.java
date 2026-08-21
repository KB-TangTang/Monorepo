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

    /**
     * 프로필 이미지 URL. 없으면 NULL — 그 경우 화면이 닉네임으로 이니셜 원을 그린다.
     *
     * <p>{@code tbl_user.profile_image_key} 를 {@code ImageStorage.urlOf()} 로 바꾼 값이다.
     * 키를 그대로 내려주지 않는다 — 저장소가 바뀌면 URL 규칙도 바뀌는데 그걸 프론트가 알면 안 된다.
     *
     * <p>2026-08-21 추가. 없던 동안 이 목록을 쓰는 화면(그룹 상세 멤버 그리드 · 채팅 아바타)이
     * 프로필을 바꿔도 계속 이니셜만 보여줬다(이슈 #407).
     */
    private String profileImageUrl;

    /** 방장 여부. */
    private boolean owner;
}

package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인한 사용자 정보. {@code GET /api/users/me} · {@code POST /api/auth/refresh} 의 {@code user} ·
 * 사용자 정보를 바꾸는 모든 PATCH 응답이 **같은 모양**을 쓴다.
 *
 * <p>⚠ **반드시 {@link #from(UserDto, String)} 으로 만든다.** 빌더를 직접 호출하는 곳이 늘면
 * 필드를 추가할 때마다 여러 군데를 고쳐야 하고, 한 곳을 빠뜨리면 그 경로에서만 값이 비는
 * 버그가 난다(실제로 name·tutorialSeenAt 을 넣을 때 두 곳을 따로 고쳐야 했다).
 * 표시명 계산 규칙도 여기 한 곳에만 둔다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMeDto {

    private Long id;
    /** 사용자가 직접 정한 표시명. **null 이면 닉네임 온보딩 미완료**다 — 프론트 가드가 이 값만 본다. */
    private String nickname;
    /** OAuth 제공자가 준 이름. 닉네임 입력창 prefill 과 표시명 fallback 에 쓴다. */
    private String socialName;
    /**
     * 프로필 이미지 URL. 미설정이면 null 이고 화면은 이니셜 아바타를 그린다.
     *
     * **서버가 완성된 URL 을 내려준다** — DB 에는 키만 있고 URL 조립은 저장소 구현이 한다.
     * 화면이 조립하지 않으므로 로컬 → S3 전환에 프론트 수정이 0 이다.
     */
    private String profileImageUrl;
    /**
     * 화면에 그릴 표시명. **서버가 `nickname ?? socialName` 을 계산해 내려준다** —
     * 규칙이 화면마다 흩어지면 어디선 닉네임, 어디선 구글 이름이 나오는 사고가 난다.
     */
    private String displayName;
    /**
     * 실명(본인확인용). 간편인증 화면이 이 값을 입력창에 미리 채운다.
     * 아직 인증을 한 번도 안 한 사용자는 null 이다. (DECISIONS.md 2026-08-11 간편인증 이름 수정 허용)
     */
    private String name;
    private String email;
    private String socialProvider;
    /**
     * 튜토리얼 완료 시각. **NULL 이면 아직 안 봤다**는 뜻이라 프론트가 이 값만 보고 노출을 정한다.
     * 별도 조회 없이 판정하도록 로그인 응답(/api/auth/refresh)에도 함께 실린다. (이슈 #128)
     */
    private java.time.LocalDateTime tutorialSeenAt;
    private java.time.LocalDateTime groupTutorialSeenAt;

    /**
     * tbl_user 행 하나를 응답 모양으로 옮긴다. 표시명은 여기서 계산한다.
     *
     * profileImageUrl 은 **밖에서 받아온다.** DTO 가 저장소(인프라)를 알지 않게 하려는 것이고,
     * 서비스가 ProfileImageUrlResolver 로 풀어 넘긴다.
     */
    public static UserMeDto from(UserDto user, String profileImageUrl) {
        return UserMeDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .socialName(user.getSocialName())
                .displayName(displayNameOf(user))
                .profileImageUrl(profileImageUrl)
                .name(user.getName())
                .email(user.getEmail())
                .socialProvider(user.getSocialProvider())
                .tutorialSeenAt(user.getTutorialSeenAt())
                .groupTutorialSeenAt(user.getGroupTutorialSeenAt())
                .build();
    }

    /**
     * 표시명 규칙 — {@code nickname ?? socialName}.
     *
     * <p>**서버에서만 계산한다.** 화면마다 각자 계산하면 어디선 닉네임, 어디선 구글 이름이
     * 나오는 사고가 난다. 실명({@code name})은 후보가 아니다 — 구글 이름이 실명과 다를 수 있어
     * 섞으면 계좌 인증 실명이 표시명을 덮어쓴다. (DECISIONS.md 2026-08-11 닉네임 온보딩)
     */
    private static String displayNameOf(UserDto user) {
        String nickname = user.getNickname();
        return nickname != null && !nickname.isBlank() ? nickname : user.getSocialName();
    }
}

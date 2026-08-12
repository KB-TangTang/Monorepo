package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * tbl_user 행 매핑.
 * 프로필 이미지는 사용자가 직접 올린 것만 저장한다(profileImageKey).
 * 구글 picture 클레임은 저장하지 않는다 — 제공자 URL 은 만료·변경되고 우리 저장소 밖에 있다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String socialProvider;   // 'GOOGLE'
    private String providerUserId;   // 구글 sub
    private String email;
    /*
     * 이름처럼 보이는 컬럼이 셋이다. 절대 섞어 쓰지 말 것 (docs/DOMAIN_GLOSSARY.md 참고).
     *   nickname   : 사용자가 직접 정한 표시명. 미설정이면 null = 온보딩 미완료
     *   socialName : OAuth 제공자가 준 이름. 입력창 prefill · 표시명 fallback
     *   name       : 실명(본인확인용). 간편인증 화면에서 채운다
     * 표시명 규칙은 nickname ?? socialName 이다. name 은 표시명 후보가 아니다 —
     * 구글 이름이 실명과 다를 수 있어 섞으면 인증 실명이 표시명을 덮어쓴다.
     */
    private String nickname;
    private String socialName;
    /**
     * 프로필 이미지 저장 키. **URL 이 아니다.** 형식은 profile/{userId}/{uuid}.jpg 다.
     * URL 은 ImageStorage 구현이 조립하므로 로컬 → S3 전환에 이 값은 바뀌지 않는다.
     * null 이면 미설정이고 화면은 이니셜 아바타를 그린다.
     */
    private String profileImageKey;
    private String name;
    private String status;           // ACTIVE / WITHDRAWN / BLOCKED
    private Long difficultyId;       // 가입 시 NORMAL(2)

    /* 튜토리얼 완료 시각. NULL 이면 아직 안 봤다는 뜻이다. (이슈 #128) */
    private java.time.LocalDateTime tutorialSeenAt;       // 메인(개인·대법원) MC_01_05
    private java.time.LocalDateTime groupTutorialSeenAt;  // 그룹(지방법원) GC_01_01
}

package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * tbl_user 행 매핑.
 * 프로필 이미지 컬럼은 스키마에 없다. 구글 picture 클레임은 저장하지 않는다.
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
    private String nickname;         // 구글 name 을 넣는다
    private String name;             // 실명. 가입 시점엔 null (계좌 인증 단계에서 채움)
    private String status;           // ACTIVE / WITHDRAWN / BLOCKED
    private Long difficultyId;       // 가입 시 EASY(1)

    /* 튜토리얼 완료 시각. NULL 이면 아직 안 봤다는 뜻이다. (이슈 #128) */
    private java.time.LocalDateTime tutorialSeenAt;       // 메인(개인·대법원) MC_01_05
    private java.time.LocalDateTime groupTutorialSeenAt;  // 그룹(지방법원) GC_01_01
}

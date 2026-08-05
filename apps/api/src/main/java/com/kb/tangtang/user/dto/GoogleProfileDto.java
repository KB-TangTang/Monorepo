package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구글 id_token 에서 뽑아낸 최소 프로필.
 * picture 클레임은 저장할 컬럼이 없으므로 받지 않는다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleProfileDto {

    private String providerUserId;   // sub — 구글 계정의 불변 고유 ID
    private String email;
    private String name;
}

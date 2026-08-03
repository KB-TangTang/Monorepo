package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** GET /api/users/me 응답. tbl_user 에 프로필 이미지 컬럼이 없으므로 포함하지 않는다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMeDto {

    private Long id;
    private String nickname;
    private String email;
}

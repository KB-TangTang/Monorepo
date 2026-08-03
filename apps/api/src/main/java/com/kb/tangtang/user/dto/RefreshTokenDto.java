package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * tbl_refresh_token 행 매핑.
 * 원문 토큰은 저장하지 않는다. tokenHash 는 SHA-256 hex 64자.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenDto {

    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean revoked;          // 컬럼 is_revoked. XML 에서 AS revoked 로 매핑한다
    private LocalDateTime revokedAt;
}

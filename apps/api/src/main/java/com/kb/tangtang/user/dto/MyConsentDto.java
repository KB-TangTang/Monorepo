package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** GET /api/consents/me 응답의 항목 1건. 마이페이지 동의관리 화면이 쓴다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyConsentDto {

    private String type;
    private boolean required;
    private String label;
    private String termsUrl;
    private boolean agreed;
    /** false 면 철회 버튼을 노출하지 않는다 (TERMS·PRIVACY). */
    private boolean withdrawable;
    private String termsVersion;
    private LocalDateTime expiresAt;
}

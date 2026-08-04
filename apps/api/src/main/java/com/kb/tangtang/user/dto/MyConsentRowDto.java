package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** tbl_user_consent 한 행을 읽기 위한 DTO. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyConsentRowDto {

    private String consentType;
    private boolean required;
    private String termsVersion;
    private int status;
    private LocalDateTime withdrawnAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** tbl_user_consent 한 행을 쓰기 위한 DTO. status 1=동의 유지, 0=철회. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecordDto {

    private Long userId;
    private String consentType;
    private boolean required;
    private String termsVersion;
    private int status;
    private LocalDateTime withdrawnAt;
    private LocalDateTime expiresAt;
}

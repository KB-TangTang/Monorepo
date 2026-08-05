package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * tbl_connected_account 한 행.
 *
 * ⚠ 원본 계좌번호를 담는 필드가 없다. 저장하지 않기 때문이다.
 *   accountNoHash  → account_no_encrypted (HMAC-SHA256, 중복 검사 전용)
 *   accountNoMasked → account_no_masked   (표시 전용)
 *
 * MyBatis 가 snake_case 컬럼을 camelCase 로 자동 매핑한다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class ConnectedAccount {

    private Long id;
    private Long userId;
    private String codefConnectedId;
    private String bankCode;
    private String bankName;
    private String accountName;
    private String accountNoEncrypted;
    private String accountNoMasked;
    private String accountType;
    private String depositTypeCode;
    private String provider;
    private BigDecimal balance;
    private String syncStatus;
    private LocalDateTime lastSyncAt;
    private String syncFailReason;
    private Boolean isActive;
    private LocalDateTime expiresAt;

    /* Lombok @Builder 와 @NoArgsConstructor 를 함께 쓰려면 전체 생성자가 필요하다. */
    public ConnectedAccount(Long id, Long userId, String codefConnectedId, String bankCode,
                            String bankName, String accountName, String accountNoEncrypted,
                            String accountNoMasked, String accountType, String depositTypeCode,
                            String provider, BigDecimal balance, String syncStatus,
                            LocalDateTime lastSyncAt, String syncFailReason, Boolean isActive,
                            LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.codefConnectedId = codefConnectedId;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountName = accountName;
        this.accountNoEncrypted = accountNoEncrypted;
        this.accountNoMasked = accountNoMasked;
        this.accountType = accountType;
        this.depositTypeCode = depositTypeCode;
        this.provider = provider;
        this.balance = balance;
        this.syncStatus = syncStatus;
        this.lastSyncAt = lastSyncAt;
        this.syncFailReason = syncFailReason;
        this.isActive = isActive;
        this.expiresAt = expiresAt;
    }
}

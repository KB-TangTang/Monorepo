package com.kb.tangtang.account.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 금융기관에서 조회한 계좌 한 건. 목서버·실 CODEF 응답을 이 타입으로 정규화한다.
 *
 * ⚠ accountNo 는 **마스킹되지 않은 전체 계좌번호**다.
 *   실 CODEF 의 resAccountDisplay 는 하이픈만 넣은 원본이다(2026-08-05 실계좌 확인).
 *   화면으로 내보내기 전에 반드시 마스킹하고, 저장할 때는 암호화한다.
 */
@Getter
@Builder
public class FinancialAccountDto {

    private final String organization;

    private final String bankName;

    /** 상품명 (CODEF resAccountName) */
    private final String accountName;

    private final String accountNo;

    /**
     * 계좌 종류를 공급자가 이미 판정해 준 경우의 값 (`DEMAND_DEPOSIT` / `SAVINGS`).
     * 목서버가 이 형태로 내려준다. 실 CODEF 는 주지 않으므로 null 이고,
     * 그때는 depositTypeCode 로 유도한다.
     */
    private final String accountType;

    /**
     * CODEF 예금 구분 코드 (`resAccountDeposit`, 예: `11`).
     *
     * ⚠ **`tbl_connected_account.deposit_type_code` 가 VARCHAR(5) 다.**
     *   여기에 `DEMAND_DEPOSIT` 같은 긴 값을 넣으면 저장이 깨진다
     *   (2026-08-05 실제 발생 — 목서버 accountType 을 이 필드에 잘못 매핑했다).
     *   코드값만 담을 것.
     */
    private final String depositTypeCode;

    private final String currency;

    private final BigDecimal balance;
}

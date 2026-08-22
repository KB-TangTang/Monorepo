package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.FinancialSyncResultDto;

import java.util.Set;

public interface FinancialSyncService {
    FinancialSyncResultDto sync(long userId);

    /**
     * 대출·페이머니처럼 계좌 연동(fetchAccounts)이 다루지 못하는 업권을 위한 확장 동기화.
     *
     * 대출·페이머니는 목서버 `/accounts` 가 아니라 별도 엔드포인트(`/loans`, `/payMoney`)로만 조회되고,
     * 이 클래스의 동기화 범위(scope)는 평소 `tbl_connected_account` 에 이미 있는 기관코드로만 정해진다.
     * 그런데 그 행은 계좌 연동(AccountLinkService.link())이 만드는데, 연동 흐름은 대출·페이머니 기관을
     * 조회할 방법이 없어 행이 생기지 않는다 — 그래서 이 두 업권은 한 번 선택돼도 범위에 영원히 들어오지
     * 못하는 순환이 생겼다(#334). extraInstitutionCodes 로 방금 선택한 기관코드를 직접 넘겨 범위를
     * 넓히면, 그 기관의 첫 동기화가 실제로 `tbl_connected_account`/`tbl_loan` 행을 만들고, 그 뒤로는
     * 평소처럼 scope 에 자연히 포함된다.
     */
    FinancialSyncResultDto sync(long userId, Set<String> extraInstitutionCodes);
}

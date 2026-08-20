package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 금융 자산 동기화 요청.
 *
 * institutionCodes 는 선택 항목이다 — 대출·페이머니처럼 계좌 연동이 다루지 못해 아직
 * `tbl_connected_account` 에 없는 기관을 이번 동기화 범위에 직접 포함시킬 때만 채운다(#334).
 * 계좌 연동 완료 직후의 최초 동기화(LinkDoneView) 가 아니면 비워 보낸다.
 */
@Getter
@Setter
public class FinancialSyncRequestDto {

    private List<String> institutionCodes;
}

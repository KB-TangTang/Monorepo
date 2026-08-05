package com.kb.tangtang.account.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 선택한 계좌를 실제로 연결하는 요청.
 * accountIds 는 계좌 목록 조회 때 서버가 부여한 임시 식별자다.
 */
@Getter
@Setter
public class LinkRequestDto {

    private String connectionId;

    private java.util.List<Long> accountIds;
}

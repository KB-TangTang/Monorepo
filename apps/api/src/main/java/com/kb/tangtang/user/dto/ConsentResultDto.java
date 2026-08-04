package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 저장·철회 응답.
 * 프론트가 저장 직후 auth 스토어의 게이트 플래그를 갱신해야 하므로
 * 별도 조회를 한 번 더 하지 않도록 여기 실어 보낸다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentResultDto {

    private boolean needsConsent;
}

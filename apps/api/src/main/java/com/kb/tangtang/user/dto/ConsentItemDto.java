package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 동의 카탈로그의 항목 1건. 약관 본문은 담지 않는다 — termsUrl 로 노션 페이지를 연다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentItemDto {

    private String type;
    private boolean required;
    private String label;
    private String termsUrl;
}

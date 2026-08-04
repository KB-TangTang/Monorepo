package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** GET /api/consents/me 응답. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyConsentListDto {

    private List<MyConsentDto> items;
}

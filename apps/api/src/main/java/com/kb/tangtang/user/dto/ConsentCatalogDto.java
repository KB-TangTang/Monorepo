package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** GET /api/consents/catalog 응답. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentCatalogDto {

    private String scope;
    private String termsVersion;
    private List<ConsentItemDto> items;
}

package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private Long parentId;
}

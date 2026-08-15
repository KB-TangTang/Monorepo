package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    /** 표준 카테고리 전체(대분류+소분류). LLM 분류 프롬프트에 선택지로 넣는다. */
    List<Category> findAll();

    /** categoryId 유효성 검증용(사용자 카테고리 수정 API). 없으면 null. */
    Category findById(@Param("id") Long id);
}

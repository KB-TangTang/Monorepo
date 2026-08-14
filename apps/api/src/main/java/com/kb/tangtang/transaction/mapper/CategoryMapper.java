package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    /** 표준 카테고리 전체(대분류+소분류). LLM 분류 프롬프트에 선택지로 넣는다. */
    List<Category> findAll();
}

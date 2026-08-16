package com.kb.tangtang.transaction.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.docs.CategoryControllerDocs;
import com.kb.tangtang.transaction.dto.CategoryListDto;
import com.kb.tangtang.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 카테고리 전체 목록을 조회한다(거래 카테고리 수동 수정 화면의 선택지). */
@RestController
@RequestMapping("/api/categories")
public class CategoryController implements CategoryControllerDocs {

    private final TransactionService service;

    public CategoryController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<CategoryListDto> getCategories(@LoginUser Long userId) {
        return ApiResponse.ok(service.getAllCategories());
    }
}

package com.kb.tangtang.transaction.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.dto.CategoryListDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code CategoryController}의 Swagger 문서. */
@Api(tags = SwaggerTags.TRANSACTION)
public interface CategoryControllerDocs {

    @ApiOperation(value = "카테고리 전체 목록",
            notes = "대분류·소분류를 평면 목록으로 반환한다. parentId가 null이면 대분류, "
                    + "아니면 그 값이 부모 대분류의 id다. 사용자마다 다르지 않은 공통 데이터지만 "
                    + "로그인 사용자만 조회할 수 있다.")
    ApiResponse<CategoryListDto> getCategories(@ApiIgnore Long userId);
}

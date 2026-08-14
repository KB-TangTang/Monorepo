package com.kb.tangtang.transaction.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateRequestDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code TransactionController}의 Swagger 문서. */
@Api(tags = SwaggerTags.TRANSACTION)
public interface TransactionControllerDocs {

    @ApiOperation(value = "거래 카테고리 수동 수정",
            notes = "categoryId로 이 거래 한 건의 카테고리를 사용자 지정(USER)으로 바꾼다. "
                    + "applyToMerchant=true면 같은 가맹점명(정규화 기준)의 이후 거래에도 최우선 적용되도록 "
                    + "tbl_user_category_map에 규칙을 등록한다 — 이미 등록된 과거 거래는 소급 반영되지 않는다. "
                    + "거래가 없거나 본인 소유가 아니면 404 NOT_FOUND, categoryId가 없는 카테고리면 "
                    + "404 CATEGORY_NOT_FOUND, categoryId 자체가 없으면 400 INVALID_REQUEST다.")
    ApiResponse<TransactionCategoryUpdateResultDto> updateCategory(
            @ApiIgnore Long userId,
            @ApiParam(value = "수정할 거래 ID", required = true, example = "501") long transactionId,
            TransactionCategoryUpdateRequestDto request);
}

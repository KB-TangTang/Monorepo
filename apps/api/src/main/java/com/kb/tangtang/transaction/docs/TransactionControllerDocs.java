package com.kb.tangtang.transaction.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateRequestDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.dto.TransactionListResponseDto;
import com.kb.tangtang.transaction.dto.TransactionMonthsDto;
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
                    + "404 CATEGORY_NOT_FOUND, categoryId 자체가 없으면 400 INVALID_REQUEST다. "
                    + "applyToMerchant=true인데 거래에 가맹점명이 없으면 400 MERCHANT_NAME_REQUIRED다.")
    ApiResponse<TransactionCategoryUpdateResultDto> updateCategory(
            @ApiIgnore Long userId,
            @ApiParam(value = "수정할 거래 ID", required = true, example = "501") long transactionId,
            TransactionCategoryUpdateRequestDto request);

    @ApiOperation(value = "장부 월 이동 목록",
            notes = "데이터가 있는 가장 이른 달(없으면 이번 달)~현재월 범위를 오름차순으로 반환한다. "
                    + "hasData=false 는 그 달에 아직 거래가 없다는 뜻(범위 안의 빈 달 또는 이번 달)이다.")
    ApiResponse<TransactionMonthsDto> getAvailableMonths(@ApiIgnore Long userId);

    @ApiOperation(value = "월별 거래내역 + 요약 조회",
            notes = "yearMonth(YYYY-MM)를 생략하면 기간 제한 없이 데이터가 있는 모든 월의 거래를 반환한다 "
                    + "(검색 화면 전용, summary는 null). yearMonth를 주면 그 달 거래 목록과 요약(summary: "
                    + "totalSpent/totalDeposit/monthOverMonthRate/paymentMethods)을 함께 반환한다. "
                    + "amount는 부호 있는 값이다 — 지출은 음수, 입금은 양수, 환불은 지출을 상계하는 양수다. "
                    + "TRANSFER(이체)도 목록에는 포함되지만 요약 합계에서는 제외된다. "
                    + "데이터가 있는 가장 이른 달보다 이전 yearMonth는 400 LEDGER_NOT_AVAILABLE, "
                    + "형식이 틀리면 400 INVALID_REQUEST다.")
    ApiResponse<TransactionListResponseDto> getTransactions(
            @ApiIgnore Long userId,
            @ApiParam(value = "조회월 YYYY-MM. 생략 시 전체 월 검색용 목록", example = "2026-07") String yearMonth);
}

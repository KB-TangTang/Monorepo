package com.kb.tangtang.fixedexpense.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseDetailDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsReportDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code FixedExpenseQueryController}의 Swagger 문서. */
@Api(tags = SwaggerTags.FIXED_EXPENSE)
public interface FixedExpenseQueryControllerDocs {

    @ApiOperation(value = "고정지출 관리 목록 조회",
            notes = "ACTIVE·미제외 항목만 반환한다. confirmedAt 유무로 후보와 확정 항목을 분리하며, "
                    + "응답은 페이지네이션 없이 현재 필터의 전체 배열을 반환한다. yearMonth는 생략하면 "
                    + "Asia/Seoul 현재월을 사용하고, 현재월 이외의 값은 NOT_FOUND다.")
    ApiResponse<FixedExpenseOverviewDto> getOverview(
            @ApiIgnore Long userId,
            @ApiParam(value = "현재 기준월(YYYY-MM, 생략 시 Asia/Seoul 현재월)", example = "2026-08")
            String yearMonth,
            @ApiParam(value = "표준 소비 카테고리 ID 필터", example = "3") Long categoryId);

    @ApiOperation(value = "고정지출 후보·확정 항목 공통 상세 조회",
            notes = "로그인 사용자가 소유한 ACTIVE·미제외 항목만 조회한다. isConfirmed로 후보·확정 여부를 "
                    + "명확히 표시하고, 최근 6개월의 유효 결제 이력을 최신순으로 최대 6건 반환한다.")
    ApiResponse<FixedExpenseDetailDto> getDetail(
            @ApiIgnore Long userId,
            @ApiParam(value = "고정지출 후보 또는 확정 항목 ID", required = true, example = "101")
            long candidateId);

    @ApiOperation(value = "고정지출 절약 감정서 조회",
            notes = "ACTIVE·미제외·확정 항목만 avgAmount 전액으로 합산한다. 월 절약 가능액은 합계, "
                    + "연 절약 가능액은 월 합계의 12배다. 후보·BUFFER·검증 취소·제외 항목은 포함하지 않는다.")
    ApiResponse<FixedExpenseSavingsReportDto> getSavingsReport(
            @ApiIgnore Long userId,
            @ApiParam(value = "현재 기준월(YYYY-MM, 생략 시 Asia/Seoul 현재월)", example = "2026-08")
            String yearMonth);
}

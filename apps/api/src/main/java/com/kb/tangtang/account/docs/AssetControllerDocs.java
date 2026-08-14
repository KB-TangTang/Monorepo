package com.kb.tangtang.account.docs;

import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code AssetController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.ACCOUNT)
public interface AssetControllerDocs {

    @ApiOperation(value = "자산 현황 요약",
            notes = "순자산·전월 대비 증감·최근 6개월 추이·구성(도넛차트)·종류별 목록을 한 번에 내려준다. "
                    + "모든 값은 응답의 asOf 시각 기준 라이브 잔액으로 계산해 화면 카드 간 금액이 어긋나지 않는다. "
                    + "baseDate 생략 시 오늘 날짜를 기준으로 한다. 형식이 잘못되면 INVALID_REQUEST(400).")
    ApiResponse<AssetSummaryDto> getSummary(@ApiIgnore Long userId,
                                            @ApiParam(value = "기준일 (YYYY-MM-DD). 생략 시 오늘")
                                            String baseDate);
}

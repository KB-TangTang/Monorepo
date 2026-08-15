package com.kb.tangtang.report.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.ChallengeMonthlyReportBatchRunDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.DEV)
public interface DevChallengeReportControllerDocs {

    @ApiOperation(value = "[DEV] 개인 챌린지 월 확정 배치 실행",
            notes = "로컬 환경에서만 선택한 YYYY-MM 월의 개인 챌린지 확정 스냅샷을 다시 계산한다. "
                    + "시연 데이터의 과거 월을 채우는 용도다.")
    ApiResponse<ChallengeMonthlyReportBatchRunDto> runMonthlyBatch(
            @ApiIgnore Long userId, String yearMonth);
}

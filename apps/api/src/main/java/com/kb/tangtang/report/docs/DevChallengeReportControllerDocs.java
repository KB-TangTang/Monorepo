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
            notes = "로컬 환경에서만 지난 YYYY-MM 월의 개인 챌린지 확정 스냅샷을 실행한다. "
                    + "force=false는 기존 확정 행을 건너뛰는 멱등 실행이고, force=true만 기존 행을 재계산한다.")
    ApiResponse<ChallengeMonthlyReportBatchRunDto> runMonthlyBatch(
            @ApiIgnore Long userId, String yearMonth, boolean force);
}

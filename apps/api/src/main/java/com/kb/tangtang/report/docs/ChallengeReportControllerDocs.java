package com.kb.tangtang.report.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.REPORT)
public interface ChallengeReportControllerDocs {

    @ApiOperation(value = "챌린지 리포트 조회 가능 월",
            notes = "유효한 챌린지 동의와 월 확정 스냅샷을 기준으로 진입 상태와 과거 확정 월만 반환한다. 당월은 제외한다.")
    ApiResponse<ChallengeReportMonthsDto> getAvailableMonths(@ApiIgnore Long userId);
}

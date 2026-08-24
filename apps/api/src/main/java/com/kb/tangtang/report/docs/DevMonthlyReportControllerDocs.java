package com.kb.tangtang.report.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.MonthlyReportBatchRunDto;
import com.kb.tangtang.report.dto.MonthlyReportManualBatchRequestDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.DEV)
public interface DevMonthlyReportControllerDocs {

    @ApiOperation(value = "[DEV/OPS] 월간 소비 리포트 수동 배치 실행",
            notes = "로컬과 배포 환경에서 실행할 수 있다. Bearer 인증 외에 `X-Report-Batch-Key` 운영 키가 필요하다.\n\n"
                    + "force=false는 일반 배치와 같이 미생성·재시도 대상만 처리한다. force=true는 "
                    + "targetUserIds에 지정한 사용자 스냅샷만 재생성하고 기존 AI 결과를 초기화한다.\n\n"
                    + "force=true에서 기존 v2 스냅샷이 없거나 이전 형식이면 당시 AI 동의를 알 수 없으므로, "
                    + "missingSnapshotAiConsents에 지정 사용자별 동의값을 빠짐없이 넣어야 한다.")
    ApiResponse<MonthlyReportBatchRunDto> runManualBatch(
            @ApiIgnore Long userId,
            @RequestHeader(value = "X-Report-Batch-Key", required = false)
            @ApiParam(value = "운영 키", required = true) String operationKey,
            @RequestBody @ApiParam(value = "실행 요청", required = true) MonthlyReportManualBatchRequestDto request);
}

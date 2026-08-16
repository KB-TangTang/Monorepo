package com.kb.tangtang.report.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.ChallengeReportDetailDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.REPORT)
public interface ChallengeReportControllerDocs {

    @ApiOperation(value = "챌린지 리포트 조회 가능 월",
            notes = "유효한 챌린지 동의와 월 확정 스냅샷을 기준으로 진입 상태와 과거 확정 월만 반환한다. 당월은 제외한다.")
    ApiResponse<ChallengeReportMonthsDto> getAvailableMonths(@ApiIgnore Long userId);

    @ApiOperation(value = "챌린지 리포트 상세",
            notes = "확정된 과거 월의 개인 미션 성과만 반환한다. 첫 리포트는 hasPreviousComparison=false와 "
                    + "monthOverMonthPercentagePoint=null로 전월 비교 없음 상태를 구분한다. "
                    + "savedAmount, overspentAmount, netSavings, annualizedNetSavings와 categoryEffects는 미션 배정 시점의 "
                    + "기준금액(B) 및 실제 지출(S)을 사용해 성공일 B-S, 실패일 max(S-B, 0)으로 계산한다. "
                    + "groupRecord는 월 확정 또는 익월 2일 보강 배치 시점에 선택 월 종료·CLOSED 조건을 만족해 저장된 그룹이 있을 때만 반환하며, "
                    + "개인 미션 성과 집계에는 포함하지 않는다. "
                    + "INVALID_YEAR_MONTH, CHALLENGE_REPORT_NOT_AVAILABLE, CHALLENGE_REPORT_NOT_FOUND 오류가 발생할 수 있다.")
    ApiResponse<ChallengeReportDetailDto> getReport(@ApiIgnore Long userId, String yearMonth);
}

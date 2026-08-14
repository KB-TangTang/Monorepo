package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code MissionScoreController} 의 Swagger 문서. */
@Api(tags = "07. 개인 미션 - 월간 점수")
public interface MissionScoreControllerDocs {

    @ApiOperation(value = "이번 달 개인 미션 누적 점수 조회",
            notes = "서울 시간 기준 현재 월의 확정 성공 미션 점수와 연속 성공 보너스를 조회한다.")
    ApiResponse<MissionMonthlyScoreDto> getCurrentMonthlyScore(@ApiIgnore Long userId);
}

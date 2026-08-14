package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionStreakDto;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code TodayMissionController} 의 Swagger 문서. */
@Api(tags = "07. 개인 미션 - 오늘의 미션 · 연속 성공일")
public interface TodayMissionControllerDocs {

    @ApiOperation(value = "오늘의 미션 조회",
            notes = "아직 배정되지 않았으면 조회 시점에 배정한다. "
                    + "미션은 **전일 정산형**이라 오늘 결과는 내일 판정된다.")
    ApiResponse<TodayMissionDto> getTodayMission(@ApiIgnore Long userId);

    @ApiOperation(value = "연속 성공일 조회", notes = "홈 화면의 연속 달성 표시에 쓴다.")
    ApiResponse<MissionStreakDto> getMissionStreak(@ApiIgnore Long userId);
}

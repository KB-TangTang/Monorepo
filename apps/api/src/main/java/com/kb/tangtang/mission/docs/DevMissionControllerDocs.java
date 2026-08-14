package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code DevMissionController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.DEV)
public interface DevMissionControllerDocs {

    @ApiOperation(value = "[DEV] 오늘의 미션 재배정",
            notes = "오늘 배정된 미션을 지우고 다시 뽑는다. **시연에서 원하는 미션이 나올 때까지 돌리는 용도다.**\n\n"
                    + "난이도·요주의 카테고리를 바꾼 뒤 결과를 바로 확인할 때도 쓴다.")
    ApiResponse<TodayMissionDto> reassignTodayMission(@ApiIgnore Long userId);
}

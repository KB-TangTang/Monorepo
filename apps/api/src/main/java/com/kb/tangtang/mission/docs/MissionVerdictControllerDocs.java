package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionVerdictAcknowledgeDto;
import com.kb.tangtang.mission.dto.MissionVerdictDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.MISSION)
public interface MissionVerdictControllerDocs {

    @ApiOperation(value = "미확인 개인 미션 판정 조회",
            notes = "로그인 사용자의 확정됐지만 아직 확인하지 않은 판정을 오래된 순서로 한 건 반환한다. "
                    + "미확인 판정이 없으면 data는 null이다.")
    ApiResponse<MissionVerdictDto> getPendingVerdict(@ApiIgnore Long userId);

    @ApiOperation(value = "개인 미션 판정 확인",
            notes = "확정 판정을 확인 처리한다. 같은 요청을 다시 보내도 최초 확인 시각을 반환한다.")
    ApiResponse<MissionVerdictAcknowledgeDto> acknowledgeVerdict(
            @ApiIgnore Long userId,
            @ApiParam(value = "미션 배정 ID", example = "123") long assignmentId);
}

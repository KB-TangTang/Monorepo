package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code MissionCategoryAnalysisController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.MISSION)
public interface MissionCategoryAnalysisControllerDocs {

    @ApiOperation(value = "요주의 카테고리 분석 조회",
            notes = "최근 28일 소비를 카테고리별로 집계해 순위를 낸 **스냅샷**이다. "
                    + "미션 배정이 이 순위를 근거로 삼는다.\n\n"
                    + "해당 주기의 미션이 모두 배정될 때까지 갱신되지 않는다. "
                    + "미션이 배정 중간에 바뀌면 사용자가 혼란스럽기 때문이다.")
    ApiResponse<MissionCategoryAnalysisDto> getCategoryAnalysis(@ApiIgnore Long userId);
}

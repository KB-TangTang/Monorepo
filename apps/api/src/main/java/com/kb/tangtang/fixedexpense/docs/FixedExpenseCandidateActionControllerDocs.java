package com.kb.tangtang.fixedexpense.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionRequestDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionResponseDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code FixedExpenseCandidateActionController}의 Swagger 문서. */
@Api(tags = SwaggerTags.FIXED_EXPENSE)
public interface FixedExpenseCandidateActionControllerDocs {

    @ApiOperation(value = "고정지출 후보 확정 또는 제외",
            notes = "본문 action은 `CONFIRM` 또는 `EXCLUDE`다. `EXCLUDE`는 실제 구독을 해지하지 않고 "
                    + "같은 패턴의 후보 재노출만 억제한다. 같은 action 재전송은 200으로 현재 상태를 반환한다. "
                    + "반대 action 또는 비활성 후보는 409 INVALID_REQUEST다.")
    ApiResponse<FixedExpenseCandidateActionResponseDto> decideCandidate(
            @ApiIgnore Long userId,
            @ApiParam(value = "처리할 후보 ID", required = true, example = "101") long candidateId,
            FixedExpenseCandidateActionRequestDto request);
}

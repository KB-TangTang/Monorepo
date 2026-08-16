package com.kb.tangtang.common.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code DevChatTriggerController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.DEV)
public interface DevChatTriggerControllerDocs {

    @ApiOperation(value = "[DEV] 재판 시스템 메시지 트리거",
            notes = "**로컬에서만 동작한다.** `DevEnvironmentGuard` 가 `app.env` 로 막는다. 인증도 필요하다.\n\n"
                    + "재판 이벤트 발행부(이슈 #169~#172)가 붙기 전까지, 같은 이벤트를 직접 쏴서 채팅방에 "
                    + "시스템 메시지가 정상 렌더링되는지 확인하는 용도다. 발행부가 붙으면 이 컨트롤러는 지운다.\n\n"
                    + "kind: `VIOLATION`(소비 위반 적발) · `TRIAL_OPENED`(재판 개시, 기본값) · "
                    + "`DEFENSE`(변론 등록) · `VERDICT`(판결 확정)")
    ApiResponse<Void> publish(
            @ApiIgnore Long userId,
            @ApiParam(value = "그룹 ID", required = true, example = "1") long groupId,
            @ApiParam(value = "기소 ID", example = "1") long indictmentId,
            @ApiParam(value = "이벤트 종류", example = "TRIAL_OPENED") String kind,
            @ApiParam(value = "대상자 닉네임", example = "절약왕") String nickname);
}

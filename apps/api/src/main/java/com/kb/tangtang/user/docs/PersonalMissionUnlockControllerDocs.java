package com.kb.tangtang.user.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.PersonalMissionUnlockDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = SwaggerTags.USER)
public interface PersonalMissionUnlockControllerDocs {

    @ApiOperation(value = "맞춤 미션 개시 안내 상태 동기화",
            notes = "요청 본문이 없다. 자격 충족 여부는 서버가 판정한다. "
                    + "데이터 부족 상태를 거친 뒤 충족되면 showUnlock=true를 반환한다.")
    ApiResponse<PersonalMissionUnlockDto> sync(@ApiIgnore Long userId);

    @ApiOperation(value = "맞춤 미션 개시 안내 확인")
    ApiResponse<PersonalMissionUnlockDto> acknowledge(@ApiIgnore Long userId);
}


package com.kb.tangtang.common.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

/** {@code HealthController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.HEALTH)
public interface HealthControllerDocs {

    @ApiOperation(value = "서버 기동 확인",
            notes = "**인증이 필요 없다.** 프론트 프록시 설정 검증에도 쓴다.\n\n"
                    + "정상이면 `{ \"success\": true, \"data\": { \"status\": \"UP\", \"service\": \"tangtang-api\" } }`")
    ApiResponse<Map<String, String>> health();
}

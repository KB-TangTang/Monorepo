package com.kb.tangtang.user.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.ConsentCatalogDto;
import com.kb.tangtang.user.dto.ConsentResultDto;
import com.kb.tangtang.user.dto.ConsentSubmitRequestDto;
import com.kb.tangtang.user.dto.MyConsentListDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code ConsentController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.USER)
public interface ConsentControllerDocs {

    @ApiOperation(
            value = "동의 항목 목록 조회",
            notes = "동의 화면이 그릴 항목과 약관 버전을 내려준다. **로그인 전에도 호출한다.**\n\n"
                    + "알 수 없는 `scope` 는 `CONSENT_TYPE_INVALID` 로 실패한다.")
    ApiResponse<ConsentCatalogDto> catalog(
            @ApiParam(value = "동의 절차 구분", required = true, allowableValues = "SIGNUP,FINANCIAL,THIRD_PARTY")
            String scope);

    @ApiOperation(
            value = "동의 제출",
            notes = "저장 직후 온보딩 게이트를 바로 갱신할 수 있도록 "
                    + "`needsConsent` 와 `needsFinancialConsent` 두 플래그를 **함께** 내려준다. "
                    + "이게 없으면 금융동의를 마쳐도 가드가 계속 동의 화면으로 되돌린다.")
    ApiResponse<ConsentResultDto> submit(@ApiIgnore Long userId,
                                         @ApiParam(value = "동의 절차와 항목별 동의 여부", required = true)
                                         ConsentSubmitRequestDto request);

    @ApiOperation(value = "내 동의 내역 조회", notes = "마이페이지의 동의 관리 화면이 쓴다.")
    ApiResponse<MyConsentListDto> myConsents(@ApiIgnore Long userId);

    @ApiOperation(
            value = "동의 철회",
            notes = "철회를 먼저 실행한 뒤 플래그를 읽는다. 순서를 바꾸면 철회 전 값이 나가 "
                    + "화면이 「아직 동의돼 있음」 으로 잘못 판단한다.")
    ApiResponse<ConsentResultDto> withdraw(@ApiIgnore Long userId,
                                           @ApiParam(value = "철회할 동의 유형", required = true) String type);
}

package com.kb.tangtang.user.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.UserMeDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/**
 * {@code TutorialController} 의 Swagger 문서.
 *
 * <p>경로는 챌린지 도메인인데 클래스가 user 패키지에 있는 이유는 완료 시각이 {@code tbl_user}
 * 에 살기 때문이다. 자기 테이블은 자기 모듈이 쓴다.
 *
 * <p>경로가 챌린지인데도 문서 태그는 {@link SwaggerTags#USER} 다. 태그는 모듈 단위로 붙이기 때문이다.
 *
 * <p>⚠ 메서드마다 {@code @ApiOperation(tags = ...)} 로 메인/그룹을 갈라 붙여 봤지만
 * <b>springfox 2.9.2 는 메서드 태그로 클래스 태그를 덮어쓰지 않고 더한다.</b>
 * 그래서 같은 엔드포인트가 두 섹션에 중복으로 나온다(2026-08-14 실측). 시도하지 말 것.
 * 정말 갈라야 한다면 컨트롤러 자체를 둘로 나누는 수밖에 없다.
 */
@Api(tags = SwaggerTags.USER)
public interface TutorialControllerDocs {

    @ApiOperation(value = "메인 챌린지 튜토리얼 완료",
            notes = "완료 시각을 남긴다. 다음 진입부터 튜토리얼이 뜨지 않는다.")
    ApiResponse<UserMeDto> completeMain(@ApiIgnore Long userId);

    @ApiOperation(value = "메인 챌린지 튜토리얼 다시 보기",
            notes = "완료 시각을 지운다. 다음 진입에서 튜토리얼이 다시 뜬다. 마이페이지의 「다시 보기」다.")
    ApiResponse<UserMeDto> resetMain(@ApiIgnore Long userId);

    @ApiOperation(value = "그룹 챌린지 튜토리얼 완료")
    ApiResponse<UserMeDto> completeGroup(@ApiIgnore Long userId);

    @ApiOperation(value = "그룹 챌린지 튜토리얼 다시 보기")
    ApiResponse<UserMeDto> resetGroup(@ApiIgnore Long userId);
}

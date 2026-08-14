package com.kb.tangtang.challenge.docs;

import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/** {@code ChallengeGroupController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.GROUP_CHALLENGE)
public interface ChallengeGroupControllerDocs {

    @ApiOperation(value = "그룹 챌린지 생성",
            notes = "**방장은 자동으로 참여자가 된다.** 별도로 참여 API 를 부를 필요가 없다.\n\n"
                    + "생성 시 초대 코드가 함께 발급된다.")
    ApiResponse<ChallengeGroupCreatedDto> create(@ApiIgnore Long userId,
                                                 @ApiParam(value = "그룹명 · 기간 · 한도 등", required = true)
                                                 ChallengeGroupCreateRequestDto request);

    @ApiOperation(value = "내가 참여 중인 그룹 목록",
            notes = "`status` 는 **반복 파라미터 또는 콤마 구분** 둘 다 된다.\n\n"
                    + "「종료됨」 탭은 `JUDGING,CLOSED` 를 함께 보낸다. 개표 중인 그룹도 종료로 묶어 보여주기 때문이다.\n"
                    + "생략하면 전체를 반환한다.")
    ApiResponse<List<ChallengeGroupDto>> findMyGroups(
            @ApiIgnore Long userId,
            @ApiParam(value = "상태 필터", allowableValues = "RECRUITING,ONGOING,JUDGING,CLOSED", allowMultiple = true)
            List<String> status);

    @ApiOperation(value = "그룹 챌린지 상세", notes = "**참여자만 볼 수 있다.** 참여자가 아니면 실패한다.")
    ApiResponse<ChallengeGroupDto> findDetail(@ApiIgnore Long userId,
                                              @ApiParam(value = "그룹 ID", required = true) Long groupId);

    @ApiOperation(value = "초대 코드 미리보기",
            notes = "참여 확인 화면이 「어떤 그룹인지」 먼저 보여주기 위해 쓴다.\n\n"
                    + "**참여할 수 없는 상태도 200 이다.** 사유는 `reason` 으로 내려간다. "
                    + "그룹 정보를 보여준 뒤 안내해야 하므로 오류로 만들지 않았다.")
    ApiResponse<InviteCodePreviewDto> previewInviteCode(@ApiIgnore Long userId,
                                                        @ApiParam(value = "초대 코드", required = true) String inviteCode);

    @ApiOperation(value = "그룹 챌린지 참여",
            notes = "참여 직후 화면이 상세로 넘어가므로 **상세 정보를 그대로 돌려준다.**")
    ApiResponse<ChallengeGroupDto> join(@ApiIgnore Long userId,
                                        @ApiParam(value = "그룹 ID", required = true) Long groupId);
}

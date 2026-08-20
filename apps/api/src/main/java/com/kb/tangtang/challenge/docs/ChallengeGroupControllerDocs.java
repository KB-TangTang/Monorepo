package com.kb.tangtang.challenge.docs;

import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupRankingDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
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
                    + "생략하면 전체를 반환한다.\n\n"
                    + "카드 배지용 `defendant`·`myVoteStatus`·`pendingTrialCount` 는 **이 목록에서만** 채워진다. "
                    + "상세·참여·초대 미리보기 응답에서는 `false`·`null`·`0` 이다.\n"
                    + "셋 다 「재판이 열려 있는지」가 아니라 **내가 지금 해야 할 일**을 가리킨다 — "
                    + "변론을 이미 냈으면 `defendant` 는 `false` 이고, 표를 다 던졌으면 `myVoteStatus` 가 `DONE` 이다. "
                    + "배지 우선순위는 `defendant` → `PENDING` → `DONE` 순이다.")
    ApiResponse<List<ChallengeGroupDto>> findMyGroups(
            @ApiIgnore Long userId,
            @ApiParam(value = "상태 필터", allowableValues = "RECRUITING,ONGOING,JUDGING,CLOSED", allowMultiple = true)
            List<String> status);

    @ApiOperation(value = "내가 처리해야 하는 재판",
            notes = "홈 「오늘의 할 일」. **변론 대기와 투표 대기를 한 배열로** 마감 임박순 정렬해 내려준다.\n\n"
                    + "`type` 은 소문자 `accuse`(내가 변론) / `vote`(내가 투표) 다. "
                    + "`type` 에 따라 비는 필드가 있다 — `accuse` 는 `amount`, "
                    + "`vote` 는 `defendantNickname`·`voteCount`·`totalVoters` 만 채워진다.\n\n"
                    + "`deadline` 은 저장된 값이 아니라 `created_at + challenge.trial.*` 계산값이다. "
                    + "**마감이 지난 건도 그대로 내려간다** — 목록에서 지우는 일은 상태 전이 배치가 한다.")
    ApiResponse<List<MyTrialDto>> findMyTrials(@ApiIgnore Long userId);

    @ApiOperation(value = "그룹 챌린지 상세", notes = "**참여자만 볼 수 있다.** 참여자가 아니면 실패한다.\n\n"
            + "그룹 정보만 준다. 상세 화면을 그리려면 아래 `/detail` 을 쓴다.")
    ApiResponse<ChallengeGroupDto> findDetail(@ApiIgnore Long userId,
                                              @ApiParam(value = "그룹 ID", required = true) Long groupId);

    @ApiOperation(value = "그룹 챌린지 상세 화면 한 벌",
            notes = "위 상세에 **재판 카드(`indictments`)와 참여자 소비 상태(`dailyMembers`)** 를 얹은 것이다. "
                    + "그룹 필드는 한 겹 없이 같은 높이로 내려간다.\n\n"
                    + "`dailyMembers` 에는 **내가 빠져 있다** — 내 몫은 `myDailyAmount`·`myUsagePercent`·"
                    + "`myRemainingAmount` 세 개다. `myRemainingAmount` 는 **초과 시 음수**다.\n\n"
                    + "`dailyAmount` 는 일일평가면 오늘 하루치, 기간평가면 기간 합계다. "
                    + "`usagePercent` 는 **100 을 넘을 수 있다**(한도 0원인 무지출 챌린지는 한 푼이라도 쓰면 100).\n\n"
                    + "`indictments` 는 진행 중(`DEFENSE_WAIT`·`VOTING`)인 기소만 오래된 순으로 준다. "
                    + "카드 종류(변론 필요/제출됨, 투표 필요/완료)는 `mine`·`status`·`myVote` 로 화면이 정한다. "
                    + "`defenseDeadline`·`voteDeadline` 은 저장값이 아니라 `created_at + challenge.trial.*` 계산값이며 "
                    + "**둘 다 채워진다.**\n\n"
                    + "`status = CLOSED` 면 종료 화면 필드가 붙는다 — `finalMembers`(확정 배치가 남긴 "
                    + "`finalRank` 오름차순 **저장값**, 재계산 없음)와 `trialStats`(확정 재판 전적). "
                    + "그 외 상태에서는 둘 다 null 이다.\n\n"
                    + "`settleTime`·`memoAuthor`·`memoDate`·채팅·`savingsAmount` 는 아직 NULL 이다.")
    ApiResponse<ChallengeGroupDetailDto> findFullDetail(@ApiIgnore Long userId,
                                                        @ApiParam(value = "그룹 ID", required = true) Long groupId);

    @ApiOperation(value = "명예 법정 — 생존자 랭킹",
            notes = "**참여자만 볼 수 있다.** 화면 한 벌을 한 번에 내려준다 — 헤더(평가 방식·한도·메모)와 "
                    + "참여자 전원의 순위.\n\n"
                    + "정렬은 요구사항정의서 6.6 — 일일평가는 남은 목숨 내림차순 → 누적 소비액 오름차순, "
                    + "기간평가는 누적 소비액 오름차순. **동률은 공동 순위**다(1, 1, 3).\n\n"
                    + "`status = CLOSED` 면 순위·부담금이 확정 배치가 남긴 **저장값**이고 다시 계산하지 않는다. "
                    + "그 외 상태에서는 조회 시점의 스냅샷이다.\n\n"
                    + "`finalOutcome`(`SURVIVED`/`ELIMINATED`)·`finalChargeAmount` 는 **CLOSED 전에는 null** 이다. "
                    + "진행 중의 목숨 0 은 「탈락 위기」이지 탈락이 아니다 — 탈락 표시 여부는 화면이 "
                    + "`status`+`finalOutcome` 으로 정한다.\n\n"
                    + "`lastSettlementDate` 는 마지막으로 결산이 끝난 날짜다(진행 중에는 오늘 제외). "
                    + "결산 행이 아직 없으면 null 이고 화면이 날짜 부분을 생략한다.\n\n"
                    + "오류 코드: `GROUP_NOT_FOUND`(그룹 없음) · `GROUP_NOT_MEMBER`(참여자 아님)")
    ApiResponse<GroupRankingDto> findRanking(@ApiIgnore Long userId,
                                             @ApiParam(value = "그룹 ID", required = true) Long groupId);

    @ApiOperation(value = "초대 코드 미리보기",
            notes = "참여 확인 화면이 「어떤 그룹인지」 먼저 보여주기 위해 쓴다.\n\n"
                    + "**참여할 수 없는 상태도 200 이다.** 사유는 `reason` 으로 내려간다. "
                    + "그룹 정보를 보여준 뒤 안내해야 하므로 오류로 만들지 않았다.")
    ApiResponse<InviteCodePreviewDto> previewInviteCode(@ApiIgnore Long userId,
                                                        @ApiParam(value = "초대 코드", required = true) String inviteCode);

    @ApiOperation(value = "그룹 챌린지 참여",
            notes = "참여 직후 화면이 상세로 넘어가므로 **상세 정보를 그대로 돌려준다.**\n\n"
                    + "**그룹 ID 가 아니라 초대 코드로 참여한다.** 코드를 아는 사람만 들어올 수 있어야 하므로 "
                    + "참여 경로를 코드 하나로 좁혔다. 코드는 대소문자를 가리지 않는다.\n\n"
                    + "오류 코드: `GROUP_INVITE_CODE_NOT_FOUND`(코드가 없거나 틀림) · "
                    + "`GROUP_ALREADY_JOINED` · `GROUP_CLOSED` · `GROUP_INVITE_CODE_EXPIRED`(모집 마감) · `GROUP_FULL`")
    ApiResponse<ChallengeGroupDto> join(@ApiIgnore Long userId,
                                        @ApiParam(value = "초대 코드", required = true) String inviteCode);

    @ApiOperation(value = "그룹 챌린지 삭제",
            notes = "**방장만, 모집 중(`RECRUITING`)일 때만** 지울 수 있다.\n\n"
                    + "시작한 뒤에는 참여자들의 소비 집계·기소·투표가 쌓여 있어 한 사람의 결정으로 남의 기록까지 "
                    + "지우게 된다. ACTIVE 이후 허용 여부는 팀 논의 대기 중이다.\n\n"
                    + "참여자·기소·투표·결산 행은 FK CASCADE 로 함께 사라지고 **채팅방도 즉시 삭제**된다. "
                    + "되돌릴 수 없다.\n\n"
                    + "**「나가기(멤버 탈퇴)」는 없다.** 목숨·순위가 참여자 수를 전제로 계산돼 중간 이탈을 "
                    + "넣으려면 그 계산을 다시 정의해야 한다.\n\n"
                    + "남은 참여자에게 삭제 알림이 간다(방장 제외). 돌려줄 게 없어 `data` 는 없다.\n\n"
                    + "오류 코드: `GROUP_NOT_FOUND`(그룹 없음) · `GROUP_NOT_OWNER`(방장 아님) · "
                    + "`GROUP_NOT_DELETABLE`(이미 시작됨)")
    ApiResponse<Void> deleteGroup(@ApiIgnore Long userId,
                                  @ApiParam(value = "그룹 ID", required = true) Long groupId);
}

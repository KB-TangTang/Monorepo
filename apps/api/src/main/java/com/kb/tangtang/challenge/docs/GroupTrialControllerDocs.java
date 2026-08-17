package com.kb.tangtang.challenge.docs;

import com.kb.tangtang.challenge.dto.GroupTrialDetailDto;
import com.kb.tangtang.challenge.dto.TrialTransactionsDto;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code GroupTrialController} 의 Swagger 문서.
 *
 * <p>태그는 {@code ChallengeGroupControllerDocs} 와 <b>같은 것</b>을 쓴다 — 태그는 모듈 단위다.
 * 컨트롤러마다 태그를 만들면 문서 섹션이 수십 개로 불어난다.
 */
@Api(tags = SwaggerTags.GROUP_CHALLENGE)
public interface GroupTrialControllerDocs {

    @ApiOperation(value = "재판 상세",
            notes = "기소 안내 · 변론 작성 · 투표 · 판결 상세가 **모두 이 응답 하나**를 쓴다. "
                    + "화면마다 엔드포인트를 두면 같은 초과액을 네 곳에서 다르게 계산하게 된다.\n\n"
                    + "**그룹 참여자면 누구나** 볼 수 있다(배심원도 봐야 한다). "
                    + "`accused.mine` 으로 내가 피고인지 가른다 — 화면이 변론 버튼과 투표 버튼을 이 값으로 고른다.\n\n"
                    + "`evalType` 이 `DAILY` 면 `challengeDate` 하루가 결산 구간이고, `PERIOD` 면 "
                    + "`startDate ~ endDate` 다. 일일결산에서도 `startDate`·`endDate` 는 그룹 기간이라 값이 있다.\n\n"
                    + "`currentAmount` 는 결산 구간의 소비액이고 **실제 부담금 입력의 상한**이다. "
                    + "`exceededAmount` = `currentAmount - limitAmount`.\n\n"
                    + "`defenseDeadline`·`voteDeadline` 은 저장값이 아니라 "
                    + "`created_at + challenge.trial.*` 계산값이다. **마감이 지난 건도 그대로 내려간다** — "
                    + "상태를 넘기는 일은 배치가 한다.\n\n"
                    + "`defense` 는 아직 변론이 없으면 **NULL** 이다(빈 객체가 아니다). "
                    + "`myVerdict` 는 내가 던진 표이고 피고 본인은 항상 NULL 이다. "
                    + "`categoryId` 가 NULL 이면 총소비 챌린지다.\n\n"
                    + "**결산 구간의 거래 목록은 여기 없다.** 상세는 배심원도 보므로 "
                    + "거래 목록은 아래 `/transactions` 로 분리했다.\n\n"
                    + "오류: `TRIAL_NOT_FOUND`(없거나 내가 그 그룹 사람이 아님 — 존재 노출을 막기 위해 구분하지 않는다)")
    ApiResponse<GroupTrialDetailDto> findTrialDetail(
            @ApiIgnore Long userId,
            @ApiParam(value = "기소 ID", required = true) Long indictmentId);

    @ApiOperation(value = "결산 구간 거래 목록 (피고 본인 전용)",
            notes = "변론 화면이 **건별로 실제 부담금을 입력받기 위해** 쓴다. "
                    + "입력값은 서버로 올라오지 않고 **합계만** 변론 등록에 전송된다.\n\n"
                    + "**기소를 발화시킨 거래 1건이 아니라 구간 전체다.** 한도 초과는 누적으로 판정되므로 "
                    + "발화 거래가 문제의 거래라는 보장이 없다 — 친구들 몫까지 대납한 큰 거래는 그 시점에 "
                    + "한도 미달이어서 기소를 만들지 않고, 뒤따르는 평범한 소비가 기소를 만든다.\n\n"
                    + "`days` 는 **거래가 있는 날만** 날짜 오름차순이다. 일일결산은 하루, 기간결산은 여러 날이 온다.\n\n"
                    + "`transactions[].amount` 는 **환불이 음수**다. 그래서 하루 목록을 더하면 "
                    + "`days[].dailyAmount` 가 나오고, 날짜별 합을 더하면 `currentAmount` 가 나온다 "
                    + "(무죄 감액이 기록된 뒤에는 `currentAmount` 만 그만큼 작다).\n\n"
                    + "`transactions[].time` 은 원천에 시각이 없으면 NULL 이고, 그 거래는 그 날 맨 위에 온다. "
                    + "`categoryName` 은 LLM 분류 전이면 NULL 이다.\n\n"
                    + "오류: `TRIAL_NOT_FOUND`(없거나 내가 피고가 아님 — 배심원에게 남의 거래내역을 "
                    + "노출하지 않으려고 권한 오류와 구분하지 않는다)")
    ApiResponse<TrialTransactionsDto> findTrialTransactions(
            @ApiIgnore Long userId,
            @ApiParam(value = "기소 ID", required = true) Long indictmentId);

    @ApiOperation(value = "변론 등록 (피고 본인 전용, multipart)",
            notes = "성공하면 기소가 `DEFENSE_WAIT` → `VOTING` 으로 넘어간다. 응답 본문은 없다.\n\n"
                    + "`multipart/form-data` 다. 파트는 `content`(필수) · `actualBurdenAmount`(필수) · "
                    + "`images`(선택, 최대 3장, 같은 이름으로 여러 개).\n\n"
                    + "`actualBurdenAmount` 는 **건별 입력의 합계**다. 건별 값은 서버로 올라오지 않는다 "
                    + "— 거래와 변론을 잇는 테이블이 없다.\n\n"
                    + "**`deductionAmount`(무죄 시 감액분)는 보내지 않는다.** 서버가 "
                    + "`currentAmount - actualBurdenAmount` 로 계산해 저장한다. 클라이언트 값을 믿지 않는다. "
                    + "실제로 소비액에서 빼 주는 시점은 무죄 확정(개표) 때다.\n\n"
                    + "증빙 사진은 긴 변 1280px JPEG 으로 다시 구워 저장한다(원본은 보관하지 않는다).\n\n"
                    + "오류: `TRIAL_NOT_FOUND`(없거나 내가 그 그룹 사람이 아님) · "
                    + "`NOT_INDICTMENT_OWNER`(내가 피고가 아님) · `DEFENSE_NOT_ALLOWED`(변론 기간이 아님) · "
                    + "`DEFENSE_CLOSED`(마감 지남) · `DEFENSE_ALREADY_EXISTS`(이미 제출) · "
                    + "`DEFENSE_CONTENT_REQUIRED` · `DEFENSE_CONTENT_TOO_LONG`(150자 초과) · "
                    + "`INVALID_BURDEN_AMOUNT`(0원 미만이거나 `currentAmount` 초과) · "
                    + "`TOO_MANY_IMAGES`(4장 이상) · `IMAGE_TOO_LARGE`(5MB 초과) · `INVALID_IMAGE`")
    ApiResponse<Void> registerDefense(
            @ApiIgnore Long userId,
            @ApiParam(value = "기소 ID", required = true) Long indictmentId,
            @ApiParam(value = "변론 내용 (최대 150자)", required = true) String content,
            @ApiParam(value = "건별 실제 부담금의 합계", required = true) BigDecimal actualBurdenAmount,
            @ApiParam(value = "증빙 사진 (최대 3장)") List<MultipartFile> images) throws IOException;

    @ApiOperation(value = "혐의 인정 (피고 본인 전용)",
            notes = "변론하지 않고 바로 유죄를 받는다. `DEFENSE_WAIT` → `GUILTY`, "
                    + "`result=1`, `verdictMethod='CONFESSION'`. 응답 본문은 없다.\n\n"
                    + "**투표·변론 행을 만들지 않는다.** 투표 없이 확정되므로 배심원에게는 판결만 보인다.\n\n"
                    + "**목숨은 여기서 차감하지 않는다.** 판결 확정 후처리(목숨·감액 반영)는 개표와 "
                    + "같은 곳에서 한 번에 한다 — 화면에도 목숨 차감 문구를 넣지 않는다.\n\n"
                    + "되돌릴 수 없다. 확정 후에는 재심·재투표가 없다.\n\n"
                    + "오류: `TRIAL_NOT_FOUND` · `NOT_INDICTMENT_OWNER` · `DEFENSE_NOT_ALLOWED` · "
                    + "`DEFENSE_CLOSED`")
    ApiResponse<Void> confess(
            @ApiIgnore Long userId,
            @ApiParam(value = "기소 ID", required = true) Long indictmentId);
}

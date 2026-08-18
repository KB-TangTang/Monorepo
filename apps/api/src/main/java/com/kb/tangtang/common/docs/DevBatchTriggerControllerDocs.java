package com.kb.tangtang.common.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDate;
import java.util.Map;

/** {@code DevBatchTriggerController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.DEV)
public interface DevBatchTriggerControllerDocs {

    @ApiOperation(value = "[DEV] 배치 즉시 실행",
            notes = "**로컬에서만 동작한다.** `DevEnvironmentGuard` 가 `app.env` 로 막는다. 인증은 필요하다.\n\n"
                    + "시연·검증용이다. 그룹 챌린지는 자정 배치 → 평가·기소 → 변론 → 투표 → 판결로 이어지는데, "
                    + "각 단계가 자정이나 수 시간 뒤에 도는 배치에 걸려 있어 **기다려서는 검증할 수 없다.**\n\n"
                    + "등록된 배치 이름: `group-challenge-status`, `group-challenge-evaluation`, "
                    + "`group-trial-deadline`, `fixed-expense-payment-reminders`\n\n"
                    + "`group-challenge-evaluation` 은 기준일을 종료 다음 날로 넣으면 "
                    + "기간평가(PERIOD) 기소까지 즉시 재현된다.\n\n"
                    + "`group-trial-deadline` 은 변론 마감이 지난 기소를 투표로 넘긴다. **기준일을 쓰지 않는다** "
                    + "— 마감은 `created_at` 과 `NOW()` 비교라, 앞당겨 보려면 `CHALLENGE_DEFENSE_HOURS` 를 "
                    + "0 으로 내리거나 `tbl_indictment.created_at` 을 과거로 UPDATE 한다. "
                    + "두 번 실행해도 결과가 같다(멱등).\n\n"
                    + "없는 이름을 넣으면 `DEV_BATCH_NOT_FOUND` 로 실패한다.")
    ApiResponse<Map<String, Object>> run(
            @ApiIgnore Long userId,
            @ApiParam(value = "배치 이름", required = true, example = "group-challenge-status") String name,
            @ApiParam(value = "기준일 (YYYY-MM-DD). 없으면 오늘. "
                    + "미래 날짜를 넣으면 그날 시작하는 챌린지까지 당겨 처리한다. "
                    + "결제 예정 알림 배치는 기준일을 받을 수 없다.", example = "2026-08-20")
            LocalDate date);

    @ApiOperation(value = "[DEV] 결제 예정 알림 초기화",
            notes = "**로컬에서만 동작한다.** 현재 사용자의 `PAYMENT_DUE` 알림과 고정지출 발송 이력만 "
                    + "지워 같은 예정일을 다시 검증할 수 있게 한다. 다른 알림은 삭제하지 않는다.")
    ApiResponse<Map<String, Integer>> resetFixedExpensePaymentReminders(@ApiIgnore Long userId);
}

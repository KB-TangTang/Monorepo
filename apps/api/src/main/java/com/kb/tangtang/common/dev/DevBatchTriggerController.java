package com.kb.tangtang.common.dev;

import com.kb.tangtang.common.docs.DevBatchTriggerControllerDocs;
import com.kb.tangtang.challenge.service.ChallengeGroupStatusBatchService;
import com.kb.tangtang.challenge.service.GroupChallengeEvaluationBatchService;
import com.kb.tangtang.challenge.service.GroupTrialDeadlineBatchService;
import com.kb.tangtang.challenge.service.GroupVerdictBatchService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.fixedexpense.service.FixedExpensePaymentReminderBatchService;
import com.kb.tangtang.fixedexpense.service.FixedExpensePaymentReminderDevService;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 배치 수동 트리거 (DEV 전용, 이슈 #152).
 *
 * <p>시연과 검증 때문에 있다. 그룹 챌린지 흐름은 자정 배치 → 평가·기소 → 변론 → 투표 → 판결로
 * 이어지는데, 각 단계가 자정이나 수 시간 뒤에 도는 배치에 걸려 있어 <b>기다려서는 검증할 수
 * 없다.</b> 이 엔드포인트로 기준일을 넣어 원하는 단계를 즉시 돌린다.
 *
 * <p><b>로컬에서만 동작한다</b> — {@link DevEnvironmentGuard} 가 {@code app.env} 로 막는다.
 * 인증도 필요하다. 인터셉터가 {@code /api/**} 에 걸려 있다.
 *
 * <p>배치가 늘어나면 {@code switch} 에 이름을 추가한다.
 * 등록 인터페이스를 두지 않은 이유는 배치가 서너 개뿐이라 이름 목록이 한눈에 보이는 편이 낫기 때문이다.
 */
@RestController
@RequestMapping("/api/dev/batches")
@Log4j2
public class DevBatchTriggerController implements DevBatchTriggerControllerDocs {

    private final DevEnvironmentGuard guard;
    private final ChallengeGroupStatusBatchService challengeGroupStatusBatchService;
    private final GroupChallengeEvaluationBatchService groupChallengeEvaluationBatchService;
    private final GroupTrialDeadlineBatchService groupTrialDeadlineBatchService;
    private final GroupVerdictBatchService groupVerdictBatchService;
    private final FixedExpensePaymentReminderBatchService paymentReminderBatchService;
    private final FixedExpensePaymentReminderDevService paymentReminderDevService;

    public DevBatchTriggerController(DevEnvironmentGuard guard,
                                     ChallengeGroupStatusBatchService challengeGroupStatusBatchService,
                                     GroupChallengeEvaluationBatchService groupChallengeEvaluationBatchService,
                                     GroupTrialDeadlineBatchService groupTrialDeadlineBatchService,
                                     GroupVerdictBatchService groupVerdictBatchService,
                                     FixedExpensePaymentReminderBatchService paymentReminderBatchService,
                                     FixedExpensePaymentReminderDevService paymentReminderDevService) {
        this.guard = guard;
        this.challengeGroupStatusBatchService = challengeGroupStatusBatchService;
        this.groupChallengeEvaluationBatchService = groupChallengeEvaluationBatchService;
        this.groupTrialDeadlineBatchService = groupTrialDeadlineBatchService;
        this.groupVerdictBatchService = groupVerdictBatchService;
        this.paymentReminderBatchService = paymentReminderBatchService;
        this.paymentReminderDevService = paymentReminderDevService;
    }

    /**
     * 배치를 즉시 실행한다.
     *
     * @param name 배치 이름. {@code group-challenge-status} · {@code group-challenge-evaluation} ·
     *             {@code group-trial-deadline} · {@code group-trial-verdict}
     * @param date 기준일. 없으면 오늘. 미래 날짜를 넣으면 그날 시작하는 챌린지까지 당겨 처리한다
     * @return 배치가 처리한 건수
     */
    @PostMapping("/{name}")
    public ApiResponse<Map<String, Object>> run(
            @LoginUser Long userId,
            @PathVariable String name,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        guard.ensureLocal();

        LocalDate baseDate = date == null ? LocalDate.now() : date;
        log.warn("DEV 배치 수동 실행 name={} baseDate={} userId={}", name, baseDate, userId);

        int affected = switch (name) {
            /*
             * 스케줄러와 같은 순서로 두 전이를 모두 돌린다(이슈 #169). 하나만 돌리는 이름을 따로 두면
             * 수동 실행과 실제 배치의 동작이 갈라져, 여기서 통과한 시나리오가 자정에 재현되지 않는다.
             * 반환값은 두 전이의 합이다.
             */
            case "group-challenge-status" -> challengeGroupStatusBatchService.startDueGroups(baseDate)
                    + challengeGroupStatusBatchService.judgeEndedGroups(baseDate);
            /*
             * 기소가 열리는지 기다리지 않고 확인하는 용도다. date 로 기준일을 바꾸면
             * "종료 다음 날" 로 넘어가 기간평가(PERIOD) 기소까지 즉시 재현할 수 있다.
             */
            case "group-challenge-evaluation" ->
                    groupChallengeEvaluationBatchService.evaluateActiveGroups(baseDate);
            /*
             * 변론 마감(이슈 #170). date 를 받지 않는다 — 마감 판정은 created_at 과 NOW() 를
             * SQL 이 직접 비교하고, 기준일을 밖에서 바꿀 수 있게 하려면 배치와 다른 쿼리를
             * 타게 된다. 앞당겨 확인하려면 CHALLENGE_DEFENSE_HOURS 를 0 으로 내리거나
             * tbl_indictment.created_at 을 과거로 UPDATE 한다.
             * 두 번 실행해도 결과가 같은지(멱등) 확인하는 용도로도 쓴다.
             */
            case "group-trial-deadline" -> groupTrialDeadlineBatchService.closeExpiredDefenses();
            /*
             * 개표(이슈 #172). 마찬가지로 date 를 받지 않는다 — 개표 대상 판정은 created_at 과
             * NOW() 비교이거나 「투표할 사람이 전부 던졌는가」다. 뒤쪽 조건 덕에 전원 투표만 끝내면
             * 마감을 기다리지 않고 바로 확인할 수 있어 시연에서 시간을 조작할 일이 거의 없다.
             * 연속으로 두 번 눌러 목숨이 한 번만 깎이는지(멱등) 확인하는 용도로도 쓴다.
             */
            case "group-trial-verdict" -> groupVerdictBatchService.confirmDueVerdicts();
            case "fixed-expense-payment-reminders" -> {
                if (date != null) {
                    throw new BusinessException("INVALID_REQUEST",
                            "결제 예정 알림 배치는 현재 날짜 기준으로만 실행할 수 있습니다.");
                }
                yield paymentReminderBatchService.sendDuePaymentReminders();
            }
            default -> throw new BusinessException("DEV_BATCH_NOT_FOUND",
                    "알 수 없는 배치 이름이에요: " + name);
        };

        return ApiResponse.ok(Map.of("batch", name, "baseDate", baseDate.toString(), "affected", affected));
    }

    /** 결제 예정 알림을 같은 결제 주기에 다시 검증할 수 있도록 현재 사용자의 테스트 흔적만 지운다. */
    @Override
    @DeleteMapping("/fixed-expense-payment-reminders")
    public ApiResponse<Map<String, Integer>> resetFixedExpensePaymentReminders(@LoginUser Long userId) {
        guard.ensureLocal();

        FixedExpensePaymentReminderDevService.ResetResult result =
                paymentReminderDevService.resetPaymentDueReminders(userId);
        log.warn("DEV 결제 예정 알림 초기화 userId={} notifications={} history={}", userId,
                result.deletedNotifications(), result.deletedReminderHistory());
        return ApiResponse.ok(Map.of(
                "deletedNotifications", result.deletedNotifications(),
                "deletedReminderHistory", result.deletedReminderHistory()));
    }
}

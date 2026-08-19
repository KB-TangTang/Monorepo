package com.kb.tangtang.common.dev;

import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.docs.DevChatTriggerControllerDocs;
import com.kb.tangtang.common.dto.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연·테스트용 시스템 메시지 트리거 (이슈 #174).
 *
 * <p>발행부(#169~#172)는 2026-08-18 에 붙었다. 그래도 남겨 두는 이유는 <b>카드 렌더링만 확인할 때</b>
 * 실제 재판을 한 판 돌릴 필요가 없어서다 — 변론 마감·투표 마감을 기다리거나 시각을 조작해야 한다.
 * 판결 카드는 유죄·무죄 도장이 갈리므로 두 종류를 각각 쏠 수 있게 해 둔다(이슈 #304).
 *
 * <p><b>로컬에서만 동작한다</b> — {@link DevEnvironmentGuard} 가 {@code app.env} 로 막는다.
 * 인증도 필요하다. 인터셉터가 {@code /api/**} 에 걸려 있다({@code DevBatchTriggerController} 와 같은
 * 이중 보호다).
 *
 * <p>SwaggerConfig 가 /api/dev/** 를 「02. 개발 전용 API」 그룹으로 자동 분리한다.
 */
@RestController
@RequestMapping("/api/dev/chat")
@Log4j2
public class DevChatTriggerController implements DevChatTriggerControllerDocs {

    private final DevEnvironmentGuard guard;
    private final ApplicationEventPublisher events;

    public DevChatTriggerController(DevEnvironmentGuard guard, ApplicationEventPublisher events) {
        this.guard = guard;
        this.events = events;
    }

    @Override
    @PostMapping("/system-message")
    public ApiResponse<Void> publish(@LoginUser Long userId,
                                     @RequestParam long groupId,
                                     @RequestParam(defaultValue = "1") long indictmentId,
                                     @RequestParam(defaultValue = "TRIAL_OPENED") String kind,
                                     @RequestParam(defaultValue = "절약왕") String nickname) {
        guard.ensureLocal();
        log.warn("DEV 재판 시스템 메시지 트리거 kind={} groupId={} indictmentId={} userId={}",
                kind, groupId, indictmentId, userId);

        switch (kind) {
            case "VIOLATION":
                events.publishEvent(new GroupTrialEvents.ViolationDetected(groupId, indictmentId, nickname));
                break;
            case "DEFENSE":
                events.publishEvent(new GroupTrialEvents.DefenseRegistered(groupId, indictmentId, nickname));
                break;
            case "VERDICT":
            case "VERDICT_GUILTY":
                events.publishEvent(GroupTrialEvents.VerdictConfirmed.byVote(groupId, indictmentId,
                        nickname + "님 재판 — 유죄. 목숨 1개가 차감됐어요.", true, 4, 2, 1));
                break;
            case "VERDICT_INNOCENT":
                events.publishEvent(GroupTrialEvents.VerdictConfirmed.byVote(groupId, indictmentId,
                        nickname + "님 재판 — 무죄. 30,000원이 소비액에서 빠졌어요.", false, 2, 4, 0));
                break;
            case "VERDICT_CONFESSION":
                events.publishEvent(GroupTrialEvents.VerdictConfirmed.byConfession(groupId, indictmentId,
                        nickname + "님이 혐의를 인정했어요. 유죄로 확정됩니다.", 1));
                break;
            default:
                events.publishEvent(new GroupTrialEvents.TrialOpened(groupId, indictmentId, nickname));
        }
        return ApiResponse.ok();
    }
}

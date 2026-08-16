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
 * <p>재판 이벤트 발행부는 #169~#172 에 있고 아직 구현되지 않았다. 그때까지 이 엔드포인트로
 * 같은 이벤트를 쏴서 채팅방 렌더링을 확인한다. 발행부가 붙으면 이 컨트롤러는 지운다.
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
                events.publishEvent(new GroupTrialEvents.VerdictConfirmed(groupId, indictmentId, "3만원 감액"));
                break;
            default:
                events.publishEvent(new GroupTrialEvents.TrialOpened(groupId, indictmentId, nickname));
        }
        return ApiResponse.ok();
    }
}

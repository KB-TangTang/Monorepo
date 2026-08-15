package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 재판 이벤트를 받아 채팅방에 봇 메시지를 남긴다 (이슈 #174).
 *
 * <p>발행부는 이슈 #169~#172 담당자의 몫이다. 이 리스너는 수신만 한다 — 재판·챌린지 서비스에는
 * 손대지 않는다.
 *
 * <p>{@code @Async} 다. 채팅 실패가 재판 로직을 되돌리면 안 된다.
 *
 * <p>알림 종류는 {@link NotificationType} 에 이미 있는 GROUP_TRIAL_OPENED · GROUP_DEFENSE_REGISTERED ·
 * GROUP_JUDGMENT 세 개를 그대로 쓴다. 소비 위반 적발(ViolationDetected)은 별도 종류가 없어
 * GROUP_TRIAL_OPENED 를 재사용한다 — 기소 후보가 곧 재판으로 이어지는 같은 흐름의 알림이라서다.
 */
@Component
public class ChatSystemMessageListener {

    private final ChatMessageService chatMessageService;

    public ChatSystemMessageListener(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Async
    @EventListener
    public void onViolationDetected(GroupTrialEvents.ViolationDetected event) {
        chatMessageService.postSystemMessage(event.getGroupId(),
                event.getTargetNickname() + "님의 소비가 적발됐어요.",
                trialLink(event.getGroupId(), event.getIndictmentId()),
                NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async
    @EventListener
    public void onTrialOpened(GroupTrialEvents.TrialOpened event) {
        chatMessageService.postSystemMessage(event.getGroupId(),
                event.getTargetNickname() + "님에 대한 재판이 열렸어요.",
                trialLink(event.getGroupId(), event.getIndictmentId()),
                NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async
    @EventListener
    public void onDefenseRegistered(GroupTrialEvents.DefenseRegistered event) {
        chatMessageService.postSystemMessage(event.getGroupId(),
                event.getTargetNickname() + "님이 변론을 등록했어요.",
                trialLink(event.getGroupId(), event.getIndictmentId()),
                NotificationType.GROUP_DEFENSE_REGISTERED);
    }

    @Async
    @EventListener
    public void onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed event) {
        chatMessageService.postSystemMessage(event.getGroupId(),
                "판결이 확정됐어요. " + event.getSummary(),
                trialLink(event.getGroupId(), event.getIndictmentId()),
                NotificationType.GROUP_JUDGMENT);
    }

    /** app:// 형식을 쓰지 않는다. 이 서비스는 웹앱이고 딥링크는 라우터 경로다 */
    private String trialLink(long groupId, long indictmentId) {
        return "/challenge/group/" + groupId + "/trial/" + indictmentId;
    }
}

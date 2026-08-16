package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatSystemMessageSpec;
import com.kb.tangtang.challenge.chat.domain.ChatSystemType;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

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
 * 다만 <b>화면이 고르는 카드는 {@link ChatSystemType} 으로 따로 구분</b>한다. 알림 종류를 재사용한다고
 * 해서 적발과 개시가 같은 카드로 보여야 하는 것은 아니다.
 */
@Component
public class ChatSystemMessageListener {

    private final ChatMessageService chatMessageService;
    private final Clock clock;

    public ChatSystemMessageListener(ChatMessageService chatMessageService) {
        this(chatMessageService, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    ChatSystemMessageListener(ChatMessageService chatMessageService, Clock clock) {
        this.chatMessageService = chatMessageService;
        this.clock = clock;
    }

    @Async
    @EventListener
    public void onViolationDetected(GroupTrialEvents.ViolationDetected event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님의 소비가 적발됐어요.",
                ChatSystemType.VIOLATION_DETECTED, NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async
    @EventListener
    public void onTrialOpened(GroupTrialEvents.TrialOpened event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님에 대한 재판이 열렸어요.",
                ChatSystemType.TRIAL_OPENED, NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async
    @EventListener
    public void onDefenseRegistered(GroupTrialEvents.DefenseRegistered event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님이 변론을 등록했어요.",
                ChatSystemType.DEFENSE_REGISTERED, NotificationType.GROUP_DEFENSE_REGISTERED);
    }

    @Async
    @EventListener
    public void onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed event) {
        post(event.getGroupId(), event.getIndictmentId(),
                "판결이 확정됐어요. " + event.getSummary(),
                ChatSystemType.VERDICT_CONFIRMED, NotificationType.GROUP_JUDGMENT);
    }

    private void post(long groupId, long indictmentId, String content,
                      ChatSystemType systemType, NotificationType notificationType) {
        chatMessageService.postSystemMessage(groupId, new ChatSystemMessageSpec(
                content, systemType, trialLink(groupId, indictmentId), caseNo(indictmentId), notificationType));
    }

    /** app:// 형식을 쓰지 않는다. 이 서비스는 웹앱이고 딥링크는 라우터 경로다 */
    private String trialLink(long groupId, long indictmentId) {
        return "/challenge/group/" + groupId + "/trial/" + indictmentId;
    }

    /**
     * 사건번호는 표시 전용이다. 기소 id 를 그대로 보여주면 "3번 사건" 처럼 읽혀 사건이 몇 건인지가
     * 드러나므로, 연도를 앞에 붙여 문서 번호처럼 만든다. 서버는 이 값을 다시 파싱하지 않는다.
     */
    private String caseNo(long indictmentId) {
        return String.format("%d-재판-%04d", LocalDate.now(clock).getYear(), indictmentId);
    }
}

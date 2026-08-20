package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatSystemMessageSpec;
import com.kb.tangtang.challenge.chat.domain.ChatSystemType;
import com.kb.tangtang.challenge.chat.domain.ChatVerdictInfo;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 재판 이벤트를 받아 채팅방에 봇 메시지를 남긴다 (이슈 #174).
 *
 * <p>이 리스너는 수신만 한다 — 재판·챌린지 서비스에는 손대지 않는다. 발행부는 각 흐름을 쥔 쪽에
 * 흩어져 있다: 적발·개시는 평가 배치({@code GroupChallengeEvaluationService#indict}, 이슈 #355),
 * 변론은 {@code DefenseService}, 판결은 {@code GroupVerdictTransitionService} 다.
 *
 * <p>{@code @Async("chatExecutor")} 다. 채팅 실패가 재판 로직을 되돌리면 안 된다.
 * <b>실행기를 공용 {@code taskExecutor} 와 분리한 것은 카드 순서 때문이다</b> — 기소 한 건이
 * 적발·개시 두 이벤트를 잇달아 쏘는데, 스레드가 둘 이상이면 두 태스크가 동시에 돌아
 * {@code ChatMessageStore.append()} 의 Redis {@code INCR} 을 먼저 잡는 쪽이 앞 번호를 가져간다.
 * 「변론이 시작됩니다」가 「한도를 넘었어요」 위에 뜨는 화면이 실제로 나온다.
 * 단일 스레드 큐라 발행 순서가 곧 표시 순서다({@code RootConfig#chatExecutor}).
 *
 * <p>{@code AFTER_COMMIT} 인 이유는 기소 INSERT 가 롤백됐는데 채팅에만 「피고인이에요」가
 * 남는 상태를 막기 위해서다. 발행부가 {@code @Transactional} 안이라 일반 {@code @EventListener}
 * 로는 커밋 전에 메시지가 나간다.
 *
 * <p>⚠ {@code fallbackExecution = true} 를 빼지 말 것. 이 코드베이스는 트랜잭션 밖 발행이
 * 섞여 있다 — {@code DefenseService#registerDefense} 는 이미지 정리 때문에 트랜잭션 밖에서
 * 발행하고(이슈 #318), 로컬 개발 트리거({@code DevChatTriggerController})에도 트랜잭션이 없다.
 * 빠뜨리면 그 이벤트들이 <b>아무 오류 없이 조용히 버려진다.</b>
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

    /* 생성자가 둘이면 표시가 없는 한 스프링이 고르지 못한다 (ChatQueryService 주석 참고) */
    @Autowired
    public ChatSystemMessageListener(ChatMessageService chatMessageService) {
        this(chatMessageService, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    ChatSystemMessageListener(ChatMessageService chatMessageService, Clock clock) {
        this.chatMessageService = chatMessageService;
        this.clock = clock;
    }

    /*
     * 문구는 "무슨 일이 일어났는지" 를 반복하지 않고 "누구에게 · 얼마나" 만 담는다.
     * 제목은 두 곳이 각자 갖고 있다 — 채팅 카드는 systemType 이, 알림은 NotificationType 이
     * (예: GROUP_JUDGMENT 의 제목이 이미 "판결이 확정됐어요" 다). 여기서 같은 문장을 다시 쓰면
     * 카드에도 알림에도 제목이 두 번 나온다.
     */
    @Async("chatExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onViolationDetected(GroupTrialEvents.ViolationDetected event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님의 소비가 한도를 넘었어요.",
                ChatSystemType.VIOLATION_DETECTED, NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async("chatExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTrialOpened(GroupTrialEvents.TrialOpened event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님이 피고인이에요. 변론이 시작됩니다.",
                ChatSystemType.TRIAL_OPENED, NotificationType.GROUP_TRIAL_OPENED);
    }

    @Async("chatExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDefenseRegistered(GroupTrialEvents.DefenseRegistered event) {
        post(event.getGroupId(), event.getIndictmentId(),
                event.getTargetNickname() + "님이 변론을 냈어요.",
                ChatSystemType.DEFENSE_REGISTERED, NotificationType.GROUP_DEFENSE_REGISTERED);
    }

    /**
     * 판결만 결과 값을 함께 싣는다(이슈 #304). 화면이 도장을 고르고 「투표 4:2 · 목숨 1 차감」을
     * 적는 데 쓴다. <b>문구에서 유죄·무죄를 다시 읽어 내지 않는다</b> — 문구를 고치면 도장이
     * 조용히 뒤집히는 구조가 되기 때문이다.
     */
    @Async("chatExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onVerdictConfirmed(GroupTrialEvents.VerdictConfirmed event) {
        chatMessageService.postSystemMessage(event.getGroupId(), new ChatSystemMessageSpec(
                event.getSummary(), ChatSystemType.VERDICT_CONFIRMED,
                trialLink(event.getGroupId(), event.getIndictmentId()),
                caseNo(event.getIndictmentId()), NotificationType.GROUP_JUDGMENT,
                verdictInfo(event)));
    }

    /** 표 분포가 없는 판결(혐의 인정)은 그 자리를 비운 채 넘긴다. 0:0 으로 채우지 않는다. */
    private ChatVerdictInfo verdictInfo(GroupTrialEvents.VerdictConfirmed event) {
        if (event.getGuiltyVotes() == null || event.getInnocentVotes() == null) {
            return ChatVerdictInfo.byConfession(event.getLivesLost());
        }
        return ChatVerdictInfo.byVote(event.isGuilty(),
                event.getGuiltyVotes(), event.getInnocentVotes(), event.getLivesLost());
    }

    private void post(long groupId, long indictmentId, String content,
                      ChatSystemType systemType, NotificationType notificationType) {
        chatMessageService.postSystemMessage(groupId, new ChatSystemMessageSpec(
                content, systemType, trialLink(groupId, indictmentId), caseNo(indictmentId), notificationType));
    }

    /**
     * app:// 형식을 쓰지 않는다. 이 서비스는 웹앱이고 딥링크는 라우터 경로다.
     *
     * <p>경로는 {@code router/index.js} 의 {@code trialProgress} 와 <b>글자 그대로 같아야 한다.</b>
     * 예전 값 {@code /challenge/group/...} 은 어느 라우트에도 맞지 않아, 카드의 「재판 보러가기」를
     * 눌러도 URL 만 바뀌고 화면이 빈 채로 남았다. 라우터는 매칭 실패를 예외로 알리지 않아
     * 콘솔에도 아무것도 남지 않는다 — 경로를 고칠 때 라우터 정의를 함께 확인할 것.
     */
    private String trialLink(long groupId, long indictmentId) {
        return "/group-challenges/" + groupId + "/trial/" + indictmentId;
    }

    /**
     * 사건번호는 표시 전용이다. 기소 id 를 그대로 보여주면 "3번 사건" 처럼 읽혀 사건이 몇 건인지가
     * 드러나므로, 연도를 앞에 붙여 문서 번호처럼 만든다. 서버는 이 값을 다시 파싱하지 않는다.
     */
    private String caseNo(long indictmentId) {
        return String.format("%d-재판-%04d", LocalDate.now(clock).getYear(), indictmentId);
    }
}

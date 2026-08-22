package com.kb.tangtang.mission.listener;

import com.kb.tangtang.mission.service.DailyMissionAssignmentService;
import com.kb.tangtang.user.event.ChallengeConsentAgreedEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@Log4j2
public class ChallengeConsentAgreedListener {

    private final DailyMissionAssignmentService assignmentService;
    private final ZoneId zoneId;

    public ChallengeConsentAgreedListener(
            DailyMissionAssignmentService assignmentService,
            @Value("${mission.assignment.zone}") String zoneId) {
        this.assignmentService = assignmentService;
        this.zoneId = ZoneId.of(zoneId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onAgreed(ChallengeConsentAgreedEvent event) {
        try {
            assignmentService.assign(event.userId(), LocalDate.now(zoneId));
        } catch (RuntimeException exception) {
            log.error("챌린지 동의 직후 개인 미션 배정 실패. userId={}", event.userId(), exception);
        }
    }
}

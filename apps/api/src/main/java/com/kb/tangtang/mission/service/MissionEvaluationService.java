package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionEvaluationTarget;
import com.kb.tangtang.mission.mapper.MissionEvaluationMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class MissionEvaluationService {

    private final MissionEvaluationMapper mapper;
    private final MissionScoreService missionScoreService;
    private final ApplicationEventPublisher events;

    public MissionEvaluationService(MissionEvaluationMapper mapper,
                                    MissionScoreService missionScoreService,
                                    ApplicationEventPublisher events) {
        this.mapper = mapper;
        this.missionScoreService = missionScoreService;
        this.events = events;
    }

    @Transactional
    public void evaluate(long assignmentId, LocalDateTime evaluatedAt) {
        MissionEvaluationTarget target = mapper.lockPendingAssignment(assignmentId);
        if (target == null) {
            return;
        }

        boolean success = amount(target.getCurrentAmount())
                .compareTo(amount(target.getTargetValue())) <= 0;
        String result = success ? "SUCCESS" : "FAIL";
        if (mapper.updateMissionResult(assignmentId, result, evaluatedAt) != 1) {
            return;
        }

        if (success) {
            mapper.increaseSuccessStreak(target.getUserId(), evaluatedAt);
        } else {
            mapper.resetStreak(target.getUserId(), evaluatedAt);
        }
        missionScoreService.recalculate(
                target.getUserId(), YearMonth.from(target.getAssignDate()));
        String verdict = success ? "어제 미션을 성공적으로 마쳤어요" : "결과를 확인해 보세요";
        events.publishEvent(new NotificationRequestedEvent(target.getUserId(), NotificationType.MISSION_VERDICT,
                java.util.Map.of("result", verdict), "/mission/personal?date=" + target.getAssignDate()));
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

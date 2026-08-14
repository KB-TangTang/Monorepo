package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionEvaluationTarget;
import com.kb.tangtang.mission.mapper.MissionEvaluationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class MissionEvaluationService {

    private final MissionEvaluationMapper mapper;
    private final MissionScoreService missionScoreService;

    public MissionEvaluationService(MissionEvaluationMapper mapper,
                                    MissionScoreService missionScoreService) {
        this.mapper = mapper;
        this.missionScoreService = missionScoreService;
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
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

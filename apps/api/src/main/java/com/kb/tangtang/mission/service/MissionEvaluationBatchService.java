package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.MissionEvaluationMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Log4j2
@Service
public class MissionEvaluationBatchService {

    private final MissionEvaluationMapper mapper;
    private final MissionEvaluationService evaluationService;

    public MissionEvaluationBatchService(MissionEvaluationMapper mapper,
                                         MissionEvaluationService evaluationService) {
        this.mapper = mapper;
        this.evaluationService = evaluationService;
    }

    public void evaluateDailyMissions(LocalDate assignDate, LocalDateTime evaluatedAt) {
        for (Long assignmentId : mapper.findPendingAssignmentIds(assignDate)) {
            evaluateSafely(assignmentId, evaluatedAt);
        }
    }

    public void evaluatePendingMissionsBefore(long userId, LocalDate beforeDate,
                                              LocalDateTime evaluatedAt) {
        for (Long assignmentId : mapper.findPendingAssignmentIdsBefore(userId, beforeDate)) {
            evaluateSafely(assignmentId, evaluatedAt);
        }
    }

    private void evaluateSafely(Long assignmentId, LocalDateTime evaluatedAt) {
        try {
            evaluationService.evaluate(assignmentId, evaluatedAt);
        } catch (RuntimeException exception) {
            log.error("개인 미션 판정 실패. assignmentId={}", assignmentId, exception);
        }
    }
}

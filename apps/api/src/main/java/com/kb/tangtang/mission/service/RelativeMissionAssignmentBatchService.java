package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Log4j2
@Service
public class RelativeMissionAssignmentBatchService {

    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final RelativeMissionAssignmentService assignmentService;

    public RelativeMissionAssignmentBatchService(RelativeMissionAssignmentMapper assignmentMapper,
                                                 RelativeMissionAssignmentService assignmentService) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentService = assignmentService;
    }

    public void assignDailyMissions(LocalDate assignDate) {
        for (Long userId : assignmentMapper.findUnassignedChallengeConsentedUserIds(assignDate)) {
            try {
                assignmentService.assign(userId, assignDate);
            } catch (BusinessException exception) {
                log.warn("상대형 미션 자동 배정 건너뜀. userId={}, code={}",
                        userId, exception.getCode());
            } catch (RuntimeException exception) {
                log.error("상대형 미션 자동 배정 실패. userId={}", userId, exception);
            }
        }
    }
}

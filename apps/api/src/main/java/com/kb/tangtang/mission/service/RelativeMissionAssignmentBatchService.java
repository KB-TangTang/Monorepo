package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Log4j2
@Service
public class RelativeMissionAssignmentBatchService {

    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final DailyMissionAssignmentService assignmentService;
    private final ApplicationEventPublisher events;

    public RelativeMissionAssignmentBatchService(RelativeMissionAssignmentMapper assignmentMapper,
                                                 DailyMissionAssignmentService assignmentService,
                                                 ApplicationEventPublisher events) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentService = assignmentService;
        this.events = events;
    }

    public void assignDailyMissions(LocalDate assignDate) {
        assignDailyMissions(assignDate, true);
    }

    public void assignDailyMissions(LocalDate assignDate, boolean notifyAssignment) {
        for (Long userId : assignmentMapper.findUnassignedChallengeConsentedUserIds(assignDate)) {
            try {
                RelativeMissionAssignmentDto assignment = assignmentService.assign(userId, assignDate);
                if (notifyAssignment && assignment != null && assignment.isAssigned()) {
                    events.publishEvent(new NotificationRequestedEvent(userId, NotificationType.MISSION_ASSIGNED,
                            java.util.Map.of("missionTitle", assignment.getMissionTitle()),
                            "/mission/personal?date=" + assignDate));
                }
            } catch (BusinessException exception) {
                log.warn("상대형 미션 자동 배정 건너뜀. userId={}, code={}",
                        userId, exception.getCode());
            } catch (RuntimeException exception) {
                log.error("상대형 미션 자동 배정 실패. userId={}", userId, exception);
            }
        }
    }
}

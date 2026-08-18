package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionDeadlineTarget;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

/** 개인 미션의 시간 기반 알림만 담당한다. 시스템 푸시는 보내지 않고 알림함·SSE에만 남긴다. */
@Component
public class MissionNotificationScheduler {

    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final MissionScoreMapper scoreMapper;
    private final ApplicationEventPublisher events;
    private final ZoneId zoneId;

    public MissionNotificationScheduler(RelativeMissionAssignmentMapper assignmentMapper,
                                        MissionScoreMapper scoreMapper,
                                        ApplicationEventPublisher events,
                                        @Value("${mission.assignment.zone}") String zoneId) {
        this.assignmentMapper = assignmentMapper;
        this.scoreMapper = scoreMapper;
        this.events = events;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${mission.notification.deadline.cron}", zone = "${mission.assignment.zone}")
    public void notifyMissionDeadline() {
        LocalDate today = LocalDate.now(zoneId);
        for (MissionDeadlineTarget target : assignmentMapper.findPendingDeadlineTargets(today)) {
            events.publishEvent(new NotificationRequestedEvent(target.getUserId(), NotificationType.MISSION_DEADLINE,
                    Map.of("content", target.getMissionTitle() + " · 자정까지"),
                    "/mission/personal?date=" + today));
        }
    }

    @Scheduled(cron = "${mission.notification.certificate.cron}", zone = "${mission.assignment.zone}")
    public void notifyPreviousMonthCertificates() {
        String yearMonth = YearMonth.now(zoneId).minusMonths(1).toString();
        String deepLink = "/mission/personal/ranking/certificate?month=" + yearMonth;
        for (Long userId : scoreMapper.findCertificateEligibleUserIds(yearMonth)) {
            events.publishEvent(new NotificationRequestedEvent(userId, NotificationType.MISSION_CERTIFICATE_ISSUED,
                    Map.of("yearMonth", yearMonth), deepLink));
        }
    }
}

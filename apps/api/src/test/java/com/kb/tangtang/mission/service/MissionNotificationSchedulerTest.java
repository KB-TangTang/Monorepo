package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionDeadlineTarget;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionNotificationSchedulerTest {

    @Mock private RelativeMissionAssignmentMapper assignmentMapper;
    @Mock private MissionScoreMapper scoreMapper;
    @Mock private ApplicationEventPublisher events;

    @Test
    void sendsCertificateNotificationAfterAiTitleBatch() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        }

        assertEquals("${MISSION_CERTIFICATE_TITLE_BATCH_CRON:0 40 0 1 * *}",
                properties.getProperty("mission.certificate.title.batch.cron"));
        assertEquals("${MISSION_NOTIFICATION_CERTIFICATE_CRON:0 50 0 1 * *}",
                properties.getProperty("mission.notification.certificate.cron"));
    }

    @Test
    void sendsDeadlineOnlyToPendingMissionTargets() {
        MissionDeadlineTarget target = new MissionDeadlineTarget();
        target.setUserId(7L);
        target.setMissionTitle("배달비 절약");
        when(assignmentMapper.findPendingDeadlineTargets(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(target));

        new MissionNotificationScheduler(assignmentMapper, scoreMapper, events, "Asia/Seoul")
                .notifyMissionDeadline();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        NotificationRequestedEvent event = (NotificationRequestedEvent) captor.getValue();
        assertEquals("MISSION_DEADLINE", event.type().name());
        assertEquals("배달비 절약 · 자정까지", event.params().get("content"));
        assertEquals("/mission/personal?date=" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")),
                event.deepLinkUrl());
    }

    @Test
    void linksPreviousMonthCertificateToActualCertificateRoute() {
        String previousMonth = YearMonth.now(java.time.ZoneId.of("Asia/Seoul")).minusMonths(1).toString();
        when(scoreMapper.findCertificateEligibleUserIds(previousMonth)).thenReturn(List.of(7L));

        new MissionNotificationScheduler(assignmentMapper, scoreMapper, events, "Asia/Seoul")
                .notifyPreviousMonthCertificates();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        NotificationRequestedEvent event = (NotificationRequestedEvent) captor.getValue();
        assertEquals("MISSION_CERTIFICATE_ISSUED", event.type().name());
        assertEquals("/mission/personal/ranking/certificate?month=" + previousMonth, event.deepLinkUrl());
    }
}

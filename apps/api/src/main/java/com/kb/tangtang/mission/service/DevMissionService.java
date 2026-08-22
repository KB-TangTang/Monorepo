package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DevMissionService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final DailyMissionAssignmentService assignmentService;
    private final TodayMissionService todayMissionService;
    private final String appEnv;
    private final Clock clock;

    @Autowired
    public DevMissionService(RelativeMissionAssignmentMapper assignmentMapper,
                             DailyMissionAssignmentService assignmentService,
                             TodayMissionService todayMissionService,
                             @Value("${app.env:local}") String appEnv) {
        this(assignmentMapper, assignmentService, todayMissionService, appEnv, Clock.system(SEOUL_ZONE));
    }

    protected DevMissionService(RelativeMissionAssignmentMapper assignmentMapper,
                                DailyMissionAssignmentService assignmentService,
                                TodayMissionService todayMissionService,
                                String appEnv,
                                Clock clock) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentService = assignmentService;
        this.todayMissionService = todayMissionService;
        this.appEnv = appEnv;
        this.clock = clock;
    }

    @Transactional
    public TodayMissionDto reassignTodayMission(long userId) {
        ensureLocalEnvironment();
        LocalDate today = LocalDate.now(clock.withZone(SEOUL_ZONE));
        assignmentMapper.deleteAssignment(userId, today);
        assignmentMapper.resetAssignedSnapshots(userId, today);
        assignmentService.assign(userId, today);
        return todayMissionService.getTodayMission(userId);
    }

    private void ensureLocalEnvironment() {
        if (!"local".equalsIgnoreCase(appEnv)) {
            throw new BusinessException("DEV_API_DISABLED", "개발용 API는 로컬 환경에서만 사용할 수 있습니다.");
        }
    }
}

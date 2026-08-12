package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.mapper.TodayMissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class TodayMissionService {

    private static final String LOW_SPENDING_NO_SPEND = "LOW_SPENDING_NO_SPEND";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final TodayMissionMapper todayMissionMapper;
    private final Clock clock;

    @Autowired
    public TodayMissionService(TodayMissionMapper todayMissionMapper) {
        this(todayMissionMapper, Clock.system(SEOUL_ZONE));
    }

    TodayMissionService(TodayMissionMapper todayMissionMapper, Clock clock) {
        this.todayMissionMapper = todayMissionMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TodayMissionDto getTodayMission(long userId) {
        LocalDate today = LocalDate.now(clock.withZone(SEOUL_ZONE));
        TodayMissionDto mission = todayMissionMapper.findTodayMission(userId, today);
        if (mission == null) {
            throw new BusinessException("TODAY_MISSION_NOT_FOUND", "오늘 배정된 미션이 없습니다.");
        }
        if (LOW_SPENDING_NO_SPEND.equals(mission.getAssignmentReason())) {
            mission.setGuideMessage(buildNoSpendGuideMessage(mission.getCategoryName()));
        }
        return mission;
    }

    private String buildNoSpendGuideMessage(String categoryName) {
        String displayName = categoryName == null || categoryName.isBlank() ? "해당 카테고리" : categoryName;
        return "평소 " + displayName + " 지출이 이미 낮아 금액을 더 나누기 어려워요. "
                + "오늘은 " + displayName + " 하루 쉬기에 도전해볼까요?";
    }
}

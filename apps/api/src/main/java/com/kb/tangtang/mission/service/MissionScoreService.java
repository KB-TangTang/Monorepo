package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class MissionScoreService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final MissionScoreMapper missionScoreMapper;
    private final Clock clock;

    @Autowired
    public MissionScoreService(MissionScoreMapper missionScoreMapper) {
        this(missionScoreMapper, Clock.system(SEOUL_ZONE));
    }

    MissionScoreService(MissionScoreMapper missionScoreMapper, Clock clock) {
        this.missionScoreMapper = missionScoreMapper;
        this.clock = clock;
    }

    @Transactional
    public void recalculate(long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        int totalScore = missionScoreMapper.calculateMonthlyScore(userId, startDate, endDate);
        missionScoreMapper.upsertMonthlyScore(userId, yearMonth.toString(), totalScore);
    }

    @Transactional(readOnly = true)
    public MissionMonthlyScoreDto getCurrentScore(long userId) {
        YearMonth yearMonth = YearMonth.now(clock.withZone(SEOUL_ZONE));
        Integer totalScore = missionScoreMapper.findMonthlyScore(userId, yearMonth.toString());
        return new MissionMonthlyScoreDto(yearMonth.toString(), totalScore == null ? 0 : totalScore);
    }
}

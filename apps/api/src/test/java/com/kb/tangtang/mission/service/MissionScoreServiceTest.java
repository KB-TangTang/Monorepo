package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionScoreServiceTest {

    private static final long USER_ID = 7L;

    @Mock private MissionScoreMapper missionScoreMapper;
    private MissionScoreService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneId.of("UTC"));
        service = new MissionScoreService(missionScoreMapper, clock);
    }

    @Test
    void recalculatesAndUpsertsWholeMonthScore() {
        when(missionScoreMapper.calculateMonthlyScore(
                USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(75);

        service.recalculate(USER_ID, java.time.YearMonth.of(2026, 8));

        verify(missionScoreMapper).upsertMonthlyScore(USER_ID, "2026-08", 75);
    }

    @Test
    void returnsCurrentSeoulMonthScore() {
        when(missionScoreMapper.findMonthlyScore(USER_ID, "2026-08")).thenReturn(120);

        MissionMonthlyScoreDto result = service.getCurrentScore(USER_ID);

        assertEquals("2026-08", result.getYearMonth());
        assertEquals(120, result.getTotalScore());
    }

    @Test
    void returnsZeroWhenMonthlyRankingDoesNotExist() {
        MissionMonthlyScoreDto result = service.getCurrentScore(USER_ID);

        assertEquals(0, result.getTotalScore());
    }
}

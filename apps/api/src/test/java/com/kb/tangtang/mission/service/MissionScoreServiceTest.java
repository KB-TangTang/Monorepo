package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.mission.domain.MissionRankingRow;
import com.kb.tangtang.mission.dto.MissionMonthlyRankingDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import java.util.List;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionScoreServiceTest {

    private static final long USER_ID = 7L;

    @Mock private MissionScoreMapper missionScoreMapper;
    @Mock private ImageStorage imageStorage;
    private MissionScoreService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneId.of("UTC"));
        service = new MissionScoreService(missionScoreMapper, imageStorage, clock);
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
        when(missionScoreMapper.findUserRanking(USER_ID, "2026-08"))
                .thenReturn(rankingRow(USER_ID, "나", null, 120, 7));
        when(missionScoreMapper.countRankingUsers("2026-08")).thenReturn(37);

        MissionMonthlyScoreDto result = service.getCurrentScore(USER_ID);

        assertEquals("2026-08", result.getYearMonth());
        assertEquals(120, result.getTotalScore());
        assertEquals(19, result.getTopPercent());
    }

    @Test
    void returnsZeroWhenMonthlyRankingDoesNotExist() {
        MissionMonthlyScoreDto result = service.getCurrentScore(USER_ID);

        assertEquals(0, result.getTotalScore());
        assertNull(result.getTopPercent());
    }

    @Test
    void returnsTopTenAndMyMonthlyRanking() {
        MissionRankingRow first = rankingRow(1L, "서영", "first.png", 1340, 1);
        MissionRankingRow me = rankingRow(USER_ID, "나", "me.png", 480, 12);
        when(missionScoreMapper.findUserRanking(USER_ID, "2026-07")).thenReturn(me);
        when(missionScoreMapper.countRankingUsers("2026-07")).thenReturn(100);
        when(missionScoreMapper.findTopRankings("2026-07", 10)).thenReturn(List.of(first));
        when(imageStorage.urlOf("first.png")).thenReturn("/images/first.png");
        when(imageStorage.urlOf("me.png")).thenReturn("/images/me.png");

        MissionMonthlyRankingDto result = service.getMonthlyRanking(USER_ID, "2026-07");

        assertEquals(100, result.getTotalUsers());
        assertEquals("/images/first.png", result.getTopRankings().get(0).getProfileImageUrl());
        assertEquals(12, result.getMyRanking().getRank());
        assertEquals(12, result.getMyRanking().getTopPercent());
    }

    @Test
    void returnsNullForCurrentSeoulMonthWhenUserHasNoRanking() {
        MissionMonthlyRankingDto result = service.getMonthlyRanking(USER_ID, null);

        assertNull(result);
        verify(missionScoreMapper).findUserRanking(USER_ID, "2026-08");
    }

    @Test
    void rejectsInvalidRankingMonth() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getMonthlyRanking(USER_ID, "2026/08")
        );

        assertEquals("INVALID_YEAR_MONTH", exception.getCode());
    }

    @Test
    void returnsAllMonthsThatHaveRankingData() {
        when(missionScoreMapper.findRankingMonths())
                .thenReturn(List.of("2026-08", "2026-07", "2025-12"));

        assertEquals(
                List.of("2026-08", "2026-07", "2025-12"),
                service.getRankingMonths().getYearMonths()
        );
    }

    private MissionRankingRow rankingRow(long userId, String nickname, String imageKey,
                                         int totalScore, int rank) {
        MissionRankingRow row = new MissionRankingRow();
        row.setUserId(userId);
        row.setNickname(nickname);
        row.setProfileImageKey(imageKey);
        row.setTotalScore(totalScore);
        row.setRank(rank);
        return row;
    }
}

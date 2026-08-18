package com.kb.tangtang.mission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.mission.domain.MissionCertificateStatsRow;
import com.kb.tangtang.mission.domain.MissionCertificateTitle;
import com.kb.tangtang.mission.domain.MissionRankingRow;
import com.kb.tangtang.mission.dto.MissionCertificateTitlesDto;
import com.kb.tangtang.mission.mapper.MissionCertificateTitleMapper;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionCertificateTitleServiceTest {

    private static final long USER_ID = 7L;

    @Mock private MissionCertificateTitleMapper titleMapper;
    @Mock private MissionScoreMapper scoreMapper;
    @Mock private MissionCertificateTitleProvider provider;
    @Mock private MissionCertificateTitleStateService stateService;
    private MissionCertificateTitleService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneId.of("UTC"));
        service = new MissionCertificateTitleService(titleMapper, scoreMapper, provider, stateService,
                new ObjectMapper(), clock, "gpt-5-nano");
    }

    @Test
    void returnsStoredAiTitles() {
        MissionCertificateTitle title = new MissionCertificateTitle();
        title.setStatus("COMPLETED");
        title.setProvider("OPENAI");
        title.setTitle1("상위 12% 절약 판결관");
        title.setTitle2("7일 연속 무죄의 지배자");
        title.setTitle3("소비 법정 MVP");
        when(scoreMapper.findUserRanking(USER_ID, "2026-07")).thenReturn(ranking());
        when(titleMapper.findByUserIdAndYearMonth(USER_ID, "2026-07")).thenReturn(title);

        MissionCertificateTitlesDto result = service.getTitles(USER_ID, "2026-07");

        assertEquals("OPENAI", result.getSource());
        assertEquals(List.of("상위 12% 절약 판결관", "7일 연속 무죄의 지배자", "소비 법정 MVP"),
                result.getTitles());
    }

    @Test
    void returnsFallbackTitlesBeforeAiGeneration() {
        when(scoreMapper.findUserRanking(USER_ID, "2026-07")).thenReturn(ranking());
        when(scoreMapper.countRankingUsers("2026-07")).thenReturn(100);

        MissionCertificateTitlesDto result = service.getTitles(USER_ID, "2026-07");

        assertEquals("FALLBACK", result.getSource());
        assertEquals("상위 12%의 판결력", result.getTitles().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesAndStoresThreeTitlesOnceClaimed() {
        when(scoreMapper.findUserRanking(USER_ID, "2026-07")).thenReturn(ranking());
        when(scoreMapper.countRankingUsers("2026-07")).thenReturn(100);
        when(scoreMapper.findCertificateStats(eq(USER_ID), any(), any())).thenReturn(stats());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        when(provider.generate(any())).thenReturn(List.of("상위 12% 절약 판결관", "7일 연속 무죄의 지배자", "소비 법정 MVP"));
        when(stateService.complete(eq(USER_ID), eq("2026-07"), org.mockito.ArgumentMatchers.<List<String>>any()))
                .thenReturn(1);

        service.generate(USER_ID, java.time.YearMonth.of(2026, 7));

        verify(titleMapper).insertIfAbsent(USER_ID, "2026-07");
        ArgumentCaptor<List<String>> titlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stateService).complete(eq(USER_ID), eq("2026-07"), titlesCaptor.capture());
        assertEquals(3, titlesCaptor.getValue().size());
    }

    private MissionRankingRow ranking() {
        MissionRankingRow row = new MissionRankingRow();
        row.setRank(12);
        row.setTotalScore(480);
        return row;
    }

    private MissionCertificateStatsRow stats() {
        MissionCertificateStatsRow stats = new MissionCertificateStatsRow();
        stats.setCompletedMissionCount(31);
        stats.setSuccessMissionCount(25);
        stats.setStreakDays(5);
        stats.setBestStreakDays(7);
        return stats;
    }
}

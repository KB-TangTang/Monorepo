package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.mapper.TodayMissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
class TodayMissionServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);

    @Mock private TodayMissionMapper todayMissionMapper;
    private TodayMissionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-11T15:30:00Z"), ZoneId.of("UTC"));
        service = new TodayMissionService(todayMissionMapper, clock);
    }

    @Test
    @DisplayName("서울 기준 오늘 날짜로 로그인 사용자의 미션을 조회한다")
    void getsTodayMissionInSeoulDate() {
        TodayMissionDto mission = new TodayMissionDto();
        mission.setMissionId(11L);
        when(todayMissionMapper.findTodayMission(USER_ID, TODAY)).thenReturn(mission);

        TodayMissionDto result = service.getTodayMission(USER_ID);

        assertEquals(11L, result.getMissionId());
        assertNull(result.getGuideMessage());
        verify(todayMissionMapper).findTodayMission(USER_ID, TODAY);
    }

    @Test
    @DisplayName("저액 목표가 무지출 미션으로 전환된 경우 안내 문구를 생성한다")
    void createsGuideMessageForLowSpendingNoSpend() {
        TodayMissionDto mission = new TodayMissionDto();
        mission.setAssignmentReason("LOW_SPENDING_NO_SPEND");
        mission.setCategoryName("카페/간식");
        when(todayMissionMapper.findTodayMission(USER_ID, TODAY)).thenReturn(mission);

        TodayMissionDto result = service.getTodayMission(USER_ID);

        assertEquals("평소 카페/간식 지출이 이미 낮아 금액을 더 나누기 어려워요. "
                + "오늘은 카페/간식 하루 쉬기에 도전해볼까요?", result.getGuideMessage());
    }

    @Test
    @DisplayName("오늘 배정된 미션이 없으면 업무 예외를 던진다")
    void throwsWhenTodayMissionDoesNotExist() {
        when(todayMissionMapper.findTodayMission(USER_ID, TODAY)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.getTodayMission(USER_ID));

        assertEquals("TODAY_MISSION_NOT_FOUND", exception.getCode());
    }
}

package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.CategorySpendingStats;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.domain.RelativeMissionAssignment;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.mission.mapper.AbsoluteMissionAssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsoluteMissionAssignmentServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDate ASSIGN_DATE = LocalDate.of(2026, 8, 18);

    @Mock private AbsoluteMissionAssignmentMapper mapper;
    @Mock private AbsoluteMissionPolicy policy;
    private AbsoluteMissionAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AbsoluteMissionAssignmentService(mapper, policy);
    }

    @Test
    void assignsCommonColdStartMissionWithUsersBaseAmount() {
        MissionPoolItem cafeMission = MissionPoolItem.builder()
                .id(101L).missionTitle("무지출 명령 · 카페").categoryId(18L).build();
        when(mapper.lockActiveUserDifficulty(USER_ID)).thenReturn(2L);
        when(mapper.findNoSpendAbsoluteMissions()).thenReturn(List.of(cafeMission));
        when(policy.coldStartMissionIndex(ASSIGN_DATE, 1)).thenReturn(0);
        when(mapper.findCategorySpendingStats(
                USER_ID, 18L, ASSIGN_DATE.minusDays(28), ASSIGN_DATE.minusDays(1)))
                .thenReturn(CategorySpendingStats.builder()
                        .baseAmount(new BigDecimal("8500.00")).build());

        RelativeMissionAssignmentDto result = service.assign(
                USER_ID, ASSIGN_DATE, AbsoluteMissionAssignmentService.COLD_START);

        assertTrue(result.isAssigned());
        assertEquals("ABSOLUTE", result.getMissionType());
        assertEquals(BigDecimal.ZERO, result.getTargetValue());
        assertNull(result.getTargetRate());
        assertEquals(new BigDecimal("8500.00"), result.getBaseAmount());
        ArgumentCaptor<RelativeMissionAssignment> assignment =
                ArgumentCaptor.forClass(RelativeMissionAssignment.class);
        verify(mapper).insertAssignment(assignment.capture());
        assertEquals(101L, assignment.getValue().getMissionId());
        assertEquals(BigDecimal.ZERO, assignment.getValue().getTargetValue());
        assertNull(assignment.getValue().getTargetRate());
        assertEquals(AbsoluteMissionAssignmentService.COLD_START,
                assignment.getValue().getAssignmentReason());
    }

    @Test
    void monthlyInspectionUsesMonthlyCommonMissionAndAllowsNullBaseAmount() {
        MissionPoolItem mission = MissionPoolItem.builder()
                .id(102L).missionTitle("무지출 명령 · 배달").categoryId(19L).build();
        when(mapper.lockActiveUserDifficulty(USER_ID)).thenReturn(3L);
        when(mapper.findNoSpendAbsoluteMissions()).thenReturn(List.of(mission));
        when(policy.monthlyMissionIndex(YearMonth.from(ASSIGN_DATE), 1)).thenReturn(0);

        RelativeMissionAssignmentDto result = service.assign(
                USER_ID, ASSIGN_DATE, AbsoluteMissionAssignmentService.MONTHLY_RANDOM);

        assertNull(result.getBaseAmount());
        ArgumentCaptor<RelativeMissionAssignment> assignment =
                ArgumentCaptor.forClass(RelativeMissionAssignment.class);
        verify(mapper).insertAssignment(assignment.capture());
        assertNull(assignment.getValue().getBaseAmount());
        assertEquals(AbsoluteMissionAssignmentService.MONTHLY_RANDOM,
                assignment.getValue().getAssignmentReason());
    }

    @Test
    void skipsWhenTodayMissionAlreadyExists() {
        when(mapper.lockActiveUserDifficulty(USER_ID)).thenReturn(2L);
        when(mapper.countAssignment(USER_ID, ASSIGN_DATE)).thenReturn(1);

        RelativeMissionAssignmentDto result = service.assign(
                USER_ID, ASSIGN_DATE, AbsoluteMissionAssignmentService.COLD_START);

        assertFalse(result.isAssigned());
        verify(mapper).lockActiveUserDifficulty(USER_ID);
        verify(mapper).countAssignment(USER_ID, ASSIGN_DATE);
        verifyNoMoreInteractions(mapper, policy);
    }
}

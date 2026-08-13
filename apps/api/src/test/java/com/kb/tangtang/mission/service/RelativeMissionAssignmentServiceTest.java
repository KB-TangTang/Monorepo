package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.CategorySpendingStats;
import com.kb.tangtang.mission.domain.MissionAnalysisSnapshot;
import com.kb.tangtang.mission.domain.MissionDifficulty;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.mission.mapper.MissionAnalysisSnapshotMapper;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelativeMissionAssignmentServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDate ASSIGN_DATE = LocalDate.of(2026, 8, 12);

    @Mock private RelativeMissionAssignmentMapper assignmentMapper;
    @Mock private MissionAnalysisSnapshotMapper snapshotMapper;
    @Mock private MissionAnalysisSnapshotService snapshotService;
    private RelativeMissionAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new RelativeMissionAssignmentService(
                assignmentMapper, snapshotMapper, snapshotService, (minimum, maximum) -> minimum);
    }

    @Test
    @DisplayName("오늘 미션이 이미 있으면 스냅샷을 소진하지 않는다")
    void skipsExistingAssignment() {
        when(assignmentMapper.lockActiveUserDifficulty(USER_ID)).thenReturn(2L);
        when(assignmentMapper.countAssignment(USER_ID, ASSIGN_DATE)).thenReturn(1);

        RelativeMissionAssignmentDto result = service.assign(USER_ID, ASSIGN_DATE);

        assertFalse(result.isAssigned());
        verifyNoInteractions(snapshotMapper, snapshotService);
    }

    @Test
    @DisplayName("계산 목표가 과거 최소 결제액보다 낮아도 계산된 상대형 목표를 배정한다")
    void assignsRelativeMissionWhenCalculatedTargetIsBelowPastMinimumPurchase() {
        MissionAnalysisSnapshot snapshot = MissionAnalysisSnapshot.builder()
                .id(31L).userId(USER_ID).categoryId(18L).categoryName("카페")
                .categoryRank(1).build();
        MissionDifficulty difficulty = MissionDifficulty.builder()
                .id(3L).difficultyName("HARD")
                .minReductionRate(new BigDecimal("50"))
                .maxReductionRate(new BigDecimal("50")).build();
        MissionPoolItem mission = MissionPoolItem.builder()
                .id(201L).missionTitle("카페 지출 절감").build();
        CategorySpendingStats stats = CategorySpendingStats.builder()
                .baseAmount(new BigDecimal("1600.00")).build();

        when(assignmentMapper.lockActiveUserDifficulty(USER_ID)).thenReturn(3L);
        when(assignmentMapper.countAssignment(USER_ID, ASSIGN_DATE)).thenReturn(0);
        when(snapshotMapper.findNextPendingSnapshotForUpdate(USER_ID)).thenReturn(snapshot);
        when(assignmentMapper.findDifficulty(3L)).thenReturn(difficulty);
        when(assignmentMapper.findLastMissionId(USER_ID, 18L)).thenReturn(null);
        when(assignmentMapper.findRelativeMissions(18L, null)).thenReturn(List.of(mission));
        when(assignmentMapper.findCategorySpendingStats(
                USER_ID, 18L, ASSIGN_DATE.minusDays(28), ASSIGN_DATE.minusDays(1))).thenReturn(stats);
        when(snapshotMapper.markAssigned(31L, ASSIGN_DATE)).thenReturn(1);

        RelativeMissionAssignmentDto result = service.assign(USER_ID, ASSIGN_DATE);

        assertTrue(result.isAssigned());
        assertEquals("RELATIVE", result.getMissionType());
        assertNull(result.getAssignmentReason());
        assertEquals(new BigDecimal("800.00"), result.getTargetValue());
        assertEquals(new BigDecimal("50"), result.getTargetRate());
        assertEquals(new BigDecimal("1600.00"), result.getBaseAmount());
        assertNull(result.getGuideMessage());
        ArgumentCaptor<com.kb.tangtang.mission.domain.RelativeMissionAssignment> captor =
                ArgumentCaptor.forClass(com.kb.tangtang.mission.domain.RelativeMissionAssignment.class);
        verify(assignmentMapper).insertAssignment(captor.capture());
        assertEquals(201L, captor.getValue().getMissionId());
        assertEquals(3L, captor.getValue().getDifficultyId());
        assertEquals(new BigDecimal("50"), captor.getValue().getTargetRate());
        assertEquals(new BigDecimal("1600.00"), captor.getValue().getBaseAmount());
        assertNull(captor.getValue().getAssignmentReason());
        verify(snapshotMapper).markAssigned(31L, ASSIGN_DATE);
    }

    @Test
    @DisplayName("계산 목표가 최소 단건 결제 금액 이상이면 절감률을 그대로 적용한 상대형 미션을 배정한다")
    void assignsCalculatedRelativeTargetWithoutClamping() {
        MissionAnalysisSnapshot snapshot = MissionAnalysisSnapshot.builder()
                .id(32L).userId(USER_ID).categoryId(19L).categoryName("배달앱")
                .categoryRank(1).build();
        when(assignmentMapper.lockActiveUserDifficulty(USER_ID)).thenReturn(2L);
        when(snapshotMapper.findNextPendingSnapshotForUpdate(USER_ID)).thenReturn(snapshot);
        when(assignmentMapper.findDifficulty(2L)).thenReturn(MissionDifficulty.builder()
                .id(2L).minReductionRate(new BigDecimal("20"))
                .maxReductionRate(new BigDecimal("40")).build());
        when(assignmentMapper.findCategorySpendingStats(
                USER_ID, 19L, ASSIGN_DATE.minusDays(28), ASSIGN_DATE.minusDays(1))).thenReturn(
                CategorySpendingStats.builder()
                        .baseAmount(new BigDecimal("10000.00")).build());
        when(assignmentMapper.findLastMissionId(USER_ID, 19L)).thenReturn(null);
        when(assignmentMapper.findRelativeMissions(19L, null)).thenReturn(List.of(
                MissionPoolItem.builder().id(103L).missionTitle("배달 현장 급습").build()));
        when(snapshotMapper.markAssigned(32L, ASSIGN_DATE)).thenReturn(1);

        RelativeMissionAssignmentDto result = service.assign(USER_ID, ASSIGN_DATE);

        assertEquals("RELATIVE", result.getMissionType());
        assertEquals(new BigDecimal("20"), result.getTargetRate());
        assertEquals(new BigDecimal("8000.00"), result.getTargetValue());
        assertNull(result.getAssignmentReason());
    }

    @Test
    @DisplayName("미소진 스냅샷이 없으면 새 주기를 만든 뒤 다시 조회한다")
    void createsSnapshotWhenCycleIsConsumed() {
        MissionAnalysisSnapshot snapshot = MissionAnalysisSnapshot.builder()
                .id(44L).userId(USER_ID).categoryId(20L).categoryRank(1).build();
        when(assignmentMapper.lockActiveUserDifficulty(USER_ID)).thenReturn(2L);
        when(snapshotMapper.findNextPendingSnapshotForUpdate(USER_ID)).thenReturn(null, snapshot);
        when(assignmentMapper.findDifficulty(2L)).thenReturn(MissionDifficulty.builder()
                .id(2L).minReductionRate(BigDecimal.ZERO).maxReductionRate(BigDecimal.ZERO).build());
        when(assignmentMapper.findLastMissionId(USER_ID, 20L)).thenReturn(null);
        when(assignmentMapper.findRelativeMissions(20L, null)).thenReturn(List.of(
                MissionPoolItem.builder().id(102L).missionTitle("미션").build()));
        when(assignmentMapper.findCategorySpendingStats(anyLong(), anyLong(), any(), any())).thenReturn(
                CategorySpendingStats.builder().baseAmount(BigDecimal.TEN).build());
        when(snapshotMapper.markAssigned(44L, ASSIGN_DATE)).thenReturn(1);

        assertTrue(service.assign(USER_ID, ASSIGN_DATE).isAssigned());

        verify(snapshotService).getOrCreateSnapshot(USER_ID);
    }
}

package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.CategoryDailySpendingStats;
import com.kb.tangtang.mission.domain.MissionAnalysisSnapshot;
import com.kb.tangtang.mission.domain.MissionDifficulty;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.domain.RelativeMissionAssignment;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.mission.mapper.MissionAnalysisSnapshotMapper;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntBinaryOperator;

@Service
public class RelativeMissionAssignmentService {

    private static final long NORMAL_DIFFICULTY_ID = 2L;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final MissionAnalysisSnapshotMapper snapshotMapper;
    private final MissionAnalysisSnapshotService snapshotService;
    private final IntBinaryOperator randomInclusive;

    @Autowired
    public RelativeMissionAssignmentService(RelativeMissionAssignmentMapper assignmentMapper,
                                            MissionAnalysisSnapshotMapper snapshotMapper,
                                            MissionAnalysisSnapshotService snapshotService) {
        this(assignmentMapper, snapshotMapper, snapshotService,
                (minimum, maximum) -> ThreadLocalRandom.current().nextInt(minimum, maximum + 1));
    }

    RelativeMissionAssignmentService(RelativeMissionAssignmentMapper assignmentMapper,
                                     MissionAnalysisSnapshotMapper snapshotMapper,
                                     MissionAnalysisSnapshotService snapshotService,
                                     IntBinaryOperator randomInclusive) {
        this.assignmentMapper = assignmentMapper;
        this.snapshotMapper = snapshotMapper;
        this.snapshotService = snapshotService;
        this.randomInclusive = randomInclusive;
    }

    @Transactional
    public RelativeMissionAssignmentDto assign(long userId, LocalDate assignDate) {
        Long difficultyId = assignmentMapper.lockActiveUserDifficulty(userId);
        if (difficultyId == null) {
            throw new BusinessException("USER_NOT_FOUND", "활성 사용자를 찾을 수 없습니다.");
        }
        if (assignmentMapper.countAssignment(userId, assignDate) > 0) {
            return RelativeMissionAssignmentDto.skipped(assignDate);
        }

        MissionAnalysisSnapshot snapshot = snapshotMapper.findNextPendingSnapshotForUpdate(userId);
        if (snapshot == null) {
            snapshotService.getOrCreateSnapshot(userId);
            snapshot = snapshotMapper.findNextPendingSnapshotForUpdate(userId);
        }
        if (snapshot == null) {
            return RelativeMissionAssignmentDto.skipped(assignDate);
        }

        long effectiveDifficultyId = difficultyId == null ? NORMAL_DIFFICULTY_ID : difficultyId;
        MissionDifficulty difficulty = assignmentMapper.findDifficulty(effectiveDifficultyId);
        if (difficulty == null) {
            difficulty = assignmentMapper.findDifficulty(NORMAL_DIFFICULTY_ID);
            effectiveDifficultyId = NORMAL_DIFFICULTY_ID;
        }
        if (difficulty == null) {
            throw new BusinessException("MISSION_DIFFICULTY_NOT_FOUND", "미션 난이도 정책을 찾을 수 없습니다.");
        }

        Long lastMissionId = assignmentMapper.findLastMissionId(userId, snapshot.getCategoryId());
        List<MissionPoolItem> missions = assignmentMapper.findRelativeMissions(
                snapshot.getCategoryId(), lastMissionId);
        if (missions.isEmpty()) {
            missions = assignmentMapper.findRelativeMissions(snapshot.getCategoryId(), null);
        }
        if (missions.isEmpty()) {
            throw new BusinessException("MISSION_POOL_EMPTY", "배정 가능한 상대형 미션이 없습니다.");
        }

        CategoryDailySpendingStats stats = assignmentMapper.findDailySpendingStats(
                userId, snapshot.getCategoryId(), assignDate.minusDays(28), assignDate.minusDays(1));
        if (stats == null || stats.getBaseAmount() == null || stats.getFloorAmount() == null) {
            throw new BusinessException("MISSION_BASE_AMOUNT_NOT_FOUND", "목표 금액을 계산할 소비 내역이 없습니다.");
        }

        int minimumRate = difficulty.getMinReductionRate().intValueExact();
        int maximumRate = difficulty.getMaxReductionRate().intValueExact();
        BigDecimal targetRate = BigDecimal.valueOf(randomInclusive.applyAsInt(minimumRate, maximumRate));
        BigDecimal calculatedTarget = stats.getBaseAmount()
                .multiply(ONE_HUNDRED.subtract(targetRate))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal targetValue = calculatedTarget.max(stats.getFloorAmount()).setScale(2, RoundingMode.HALF_UP);
        MissionPoolItem mission = missions.get(randomInclusive.applyAsInt(0, missions.size() - 1));

        RelativeMissionAssignment assignment = RelativeMissionAssignment.builder()
                .userId(userId)
                .missionId(mission.getId())
                .difficultyId(effectiveDifficultyId)
                .assignDate(assignDate)
                .targetRate(targetRate)
                .targetValue(targetValue)
                .baseAmount(stats.getBaseAmount())
                .build();
        assignmentMapper.insertAssignment(assignment);
        if (snapshotMapper.markAssigned(snapshot.getId(), assignDate) != 1) {
            throw new BusinessException("MISSION_SNAPSHOT_CONFLICT", "이미 사용된 분석 결과입니다.");
        }

        return RelativeMissionAssignmentDto.builder()
                .assigned(true)
                .missionId(mission.getId())
                .missionTitle(mission.getMissionTitle())
                .categoryId(snapshot.getCategoryId())
                .assignDate(assignDate)
                .targetRate(targetRate)
                .targetValue(targetValue)
                .baseAmount(stats.getBaseAmount())
                .build();
    }
}

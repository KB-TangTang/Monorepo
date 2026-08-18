package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.CategorySpendingStats;
import com.kb.tangtang.mission.domain.MissionPoolItem;
import com.kb.tangtang.mission.domain.RelativeMissionAssignment;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.mission.mapper.AbsoluteMissionAssignmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class AbsoluteMissionAssignmentService {

    public static final String COLD_START = "COLD_START";
    public static final String MONTHLY_RANDOM = "MONTHLY_RANDOM";

    private final AbsoluteMissionAssignmentMapper assignmentMapper;
    private final AbsoluteMissionPolicy policy;

    public AbsoluteMissionAssignmentService(AbsoluteMissionAssignmentMapper assignmentMapper,
                                            AbsoluteMissionPolicy policy) {
        this.assignmentMapper = assignmentMapper;
        this.policy = policy;
    }

    @Transactional
    public RelativeMissionAssignmentDto assign(long userId, LocalDate assignDate, String assignmentReason) {
        Long difficultyId = assignmentMapper.lockActiveUserDifficulty(userId);
        if (difficultyId == null) {
            throw new BusinessException("USER_NOT_FOUND", "활성 사용자를 찾을 수 없습니다.");
        }
        if (assignmentMapper.countAssignment(userId, assignDate) > 0) {
            return RelativeMissionAssignmentDto.skipped(assignDate);
        }

        List<MissionPoolItem> candidates = assignmentMapper.findNoSpendAbsoluteMissions();
        if (candidates.isEmpty()) {
            throw new BusinessException("MISSION_POOL_EMPTY", "배정 가능한 무지출 절대형 미션이 없습니다.");
        }
        int index = MONTHLY_RANDOM.equals(assignmentReason)
                ? policy.monthlyMissionIndex(YearMonth.from(assignDate), candidates.size())
                : policy.coldStartMissionIndex(assignDate, candidates.size());
        MissionPoolItem mission = candidates.get(index);

        CategorySpendingStats stats = assignmentMapper.findCategorySpendingStats(
                userId, mission.getCategoryId(), assignDate.minusDays(28), assignDate.minusDays(1));
        BigDecimal baseAmount = stats == null ? null : stats.getBaseAmount();

        RelativeMissionAssignment assignment = RelativeMissionAssignment.builder()
                .userId(userId)
                .missionId(mission.getId())
                .difficultyId(difficultyId)
                .assignDate(assignDate)
                .targetRate(null)
                .targetValue(BigDecimal.ZERO)
                .baseAmount(baseAmount)
                .assignmentReason(assignmentReason)
                .build();
        assignmentMapper.insertAssignment(assignment);

        return RelativeMissionAssignmentDto.builder()
                .assigned(true)
                .missionId(mission.getId())
                .missionTitle(mission.getMissionTitle())
                .categoryId(mission.getCategoryId())
                .assignDate(assignDate)
                .targetValue(BigDecimal.ZERO)
                .baseAmount(baseAmount)
                .missionType("ABSOLUTE")
                .assignmentReason(assignmentReason)
                .build();
    }
}

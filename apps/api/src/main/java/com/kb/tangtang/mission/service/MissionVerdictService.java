package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.MissionVerdictRow;
import com.kb.tangtang.mission.dto.MissionVerdictAcknowledgeDto;
import com.kb.tangtang.mission.dto.MissionVerdictDto;
import com.kb.tangtang.mission.dto.WeeklyMissionResultDto;
import com.kb.tangtang.mission.mapper.MissionVerdictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class MissionVerdictService {

    private static final int STREAK_BONUS_POINTS = 5;

    private final MissionVerdictMapper mapper;
    private final Clock clock;

    @Autowired
    public MissionVerdictService(
            MissionVerdictMapper mapper,
            @Value("${mission.zone:Asia/Seoul}") String zoneId) {
        this(mapper, Clock.system(ZoneId.of(zoneId)));
    }

    MissionVerdictService(MissionVerdictMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MissionVerdictDto getPendingVerdict(long userId) {
        LocalDate verdictDate = LocalDate.now(clock).minusDays(1);
        MissionVerdictRow row = mapper.findOldestUncheckedVerdict(userId, verdictDate);
        if (row == null) {
            return null;
        }

        BigDecimal currentAmount = amount(row.getCurrentAmount());
        BigDecimal targetValue = amount(row.getTargetValue());
        boolean success = "SUCCESS".equals(row.getResult());
        int points = success && row.getBasePoints() != null ? row.getBasePoints() : 0;
        int bonusPoints = success && Boolean.TRUE.equals(row.getPreviousSuccess())
                ? STREAK_BONUS_POINTS : 0;

        return MissionVerdictDto.builder()
                .assignmentId(row.getAssignmentId())
                .result(row.getResult())
                .assignDate(row.getAssignDate())
                .categoryName(row.getCategoryName())
                .currentAmount(currentAmount)
                .targetValue(targetValue)
                .remainAmount(targetValue.subtract(currentAmount).max(BigDecimal.ZERO))
                .overAmount(currentAmount.subtract(targetValue).max(BigDecimal.ZERO))
                .points(points)
                .bonusPoints(bonusPoints)
                .streakDays(calculateStreakDays(
                        mapper.findResultsThroughDate(userId, row.getAssignDate()), row.getAssignDate()))
                .pendingCount(Math.max(mapper.countUncheckedVerdicts(userId, verdictDate) - 1, 0))
                .transactions(mapper.findVerdictTransactions(row.getAssignmentId(), userId))
                .build();
    }

    @Transactional
    public MissionVerdictAcknowledgeDto acknowledge(long userId, long assignmentId) {
        if (mapper.countOwnedFinalizedVerdict(assignmentId, userId) == 0) {
            throw new BusinessException(
                    "NOT_FOUND", "확인할 개인 미션 판정 결과를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        LocalDateTime checkedAt = LocalDateTime.now(clock).withNano(0);
        mapper.acknowledgeVerdict(assignmentId, userId, checkedAt);
        LocalDateTime storedCheckedAt = mapper.findResultCheckedAt(assignmentId, userId);
        return new MissionVerdictAcknowledgeDto(assignmentId, storedCheckedAt);
    }

    private int calculateStreakDays(List<WeeklyMissionResultDto> results, LocalDate endDate) {
        int streakDays = 0;
        LocalDate expectedDate = endDate;
        for (WeeklyMissionResultDto result : results) {
            if (!expectedDate.equals(result.getDate()) || !"SUCCESS".equals(result.getResult())) {
                break;
            }
            streakDays += 1;
            expectedDate = expectedDate.minusDays(1);
        }
        return streakDays;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

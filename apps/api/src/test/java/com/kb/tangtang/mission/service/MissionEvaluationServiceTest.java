package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionEvaluationTarget;
import com.kb.tangtang.mission.mapper.MissionEvaluationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionEvaluationServiceTest {

    private static final LocalDateTime EVALUATED_AT = LocalDateTime.of(2026, 8, 14, 0, 0);

    @Mock MissionEvaluationMapper mapper;
    MissionEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new MissionEvaluationService(mapper);
    }

    @Test
    void increasesStreakWhenSpendingIsAtOrBelowTarget() {
        MissionEvaluationTarget target = target("15000", "15000");
        when(mapper.lockPendingAssignment(11L)).thenReturn(target);
        when(mapper.updateMissionResult(11L, "SUCCESS", EVALUATED_AT)).thenReturn(1);

        service.evaluate(11L, EVALUATED_AT);

        InOrder order = inOrder(mapper);
        order.verify(mapper).updateMissionResult(11L, "SUCCESS", EVALUATED_AT);
        order.verify(mapper).increaseSuccessStreak(7L, EVALUATED_AT);
        verify(mapper, never()).resetStreak(7L, EVALUATED_AT);
    }

    @Test
    void resetsStreakWhenSpendingExceedsTarget() {
        MissionEvaluationTarget target = target("15001", "15000");
        when(mapper.lockPendingAssignment(11L)).thenReturn(target);
        when(mapper.updateMissionResult(11L, "FAIL", EVALUATED_AT)).thenReturn(1);

        service.evaluate(11L, EVALUATED_AT);

        verify(mapper).resetStreak(7L, EVALUATED_AT);
        verify(mapper, never()).increaseSuccessStreak(7L, EVALUATED_AT);
    }

    @Test
    void ignoresAssignmentAlreadyEvaluatedByAnotherWorker() {
        when(mapper.lockPendingAssignment(11L)).thenReturn(null);

        service.evaluate(11L, EVALUATED_AT);

        verify(mapper, never()).updateMissionResult(11L, "SUCCESS", EVALUATED_AT);
        verify(mapper, never()).updateMissionResult(11L, "FAIL", EVALUATED_AT);
    }

    private MissionEvaluationTarget target(String currentAmount, String targetValue) {
        MissionEvaluationTarget target = new MissionEvaluationTarget();
        target.setAssignmentId(11L);
        target.setUserId(7L);
        target.setCurrentAmount(new BigDecimal(currentAmount));
        target.setTargetValue(new BigDecimal(targetValue));
        return target;
    }
}

package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 평가·기소 배치 본체 (이슈 #168). 그룹 루프와 실패 격리만 본다. */
@ExtendWith(MockitoExtension.class)
class GroupChallengeEvaluationBatchServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Mock private ChallengeGroupMapper groupMapper;
    @Mock private GroupChallengeEvaluationService evaluationService;

    @InjectMocks private GroupChallengeEvaluationBatchService batchService;

    @Test
    @DisplayName("어제를 조회 조건에 넣는다 — end_date 가 어제인 그룹까지 남아야 마지막 날 심야 거래를 잡는다")
    void looksBackOneDay() {
        when(groupMapper.findGroupsToEvaluate("ACTIVE", TODAY, TODAY.minusDays(1)))
                .thenReturn(List.of());

        assertEquals(0, batchService.evaluateActiveGroups(TODAY));

        verify(groupMapper).findGroupsToEvaluate("ACTIVE", TODAY, TODAY.minusDays(1));
        verify(evaluationService, never()).evaluate(any(), any());
    }

    @Test
    @DisplayName("한 그룹이 실패해도 나머지는 계속 처리하고 성공분의 기소 수만 센다")
    void oneFailureDoesNotStopTheRest() {
        ChallengeGroup first = group(1L);
        ChallengeGroup second = group(2L);
        ChallengeGroup third = group(3L);
        when(groupMapper.findGroupsToEvaluate("ACTIVE", TODAY, TODAY.minusDays(1)))
                .thenReturn(List.of(first, second, third));
        when(evaluationService.evaluate(first, TODAY)).thenReturn(1);
        when(evaluationService.evaluate(second, TODAY)).thenThrow(new IllegalStateException("boom"));
        when(evaluationService.evaluate(third, TODAY)).thenReturn(2);

        assertEquals(3, batchService.evaluateActiveGroups(TODAY));

        verify(evaluationService).evaluate(third, TODAY);
    }

    private ChallengeGroup group(long id) {
        return ChallengeGroup.builder().id(id).status("ACTIVE").build();
    }
}

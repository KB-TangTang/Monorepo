package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 상태 전이 배치 본체 (이슈 #152).
 *
 * <p>배치가 책임지는 건 "대상을 뽑아 한 건씩 돌리고, 한 건이 터져도 멈추지 않는 것" 뿐이다.
 * 전이 규칙 자체는 {@link ChallengeGroupStatusTransitionServiceTest} 가 본다.
 *
 * <p>시작일이 아직 안 온 그룹을 거르는 일은 {@code findGroupsToStart} 의 SQL 이 한다.
 * 단위 테스트로는 검증할 수 없어, 여기서는 <b>배치가 RECRUITING 과 기준일을 그대로
 * 넘기는지</b>까지만 본다. SQL 자체는 수동 확인 절차로 남겼다(리뷰 문서 참고).
 */
@ExtendWith(MockitoExtension.class)
class ChallengeGroupStatusBatchServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 13);

    @Mock private ChallengeGroupMapper groupMapper;
    @Mock private ChallengeGroupStatusTransitionService transitionService;

    @Test
    @DisplayName("모집 중이면서 시작일이 도래한 그룹만 조회한다")
    void queriesRecruitingGroupsOnBaseDate() {
        when(groupMapper.findGroupsToStart("RECRUITING", TODAY)).thenReturn(List.of());

        assertEquals(0, batch().startDueGroups(TODAY));

        verify(groupMapper).findGroupsToStart("RECRUITING", TODAY);
        verifyNoInteractions(transitionService);
    }

    @Test
    @DisplayName("한 그룹이 터져도 나머지 그룹은 계속 처리한다")
    void oneGroupFailureDoesNotStopTheRest() {
        ChallengeGroup first = group(1L);
        ChallengeGroup broken = group(2L);
        ChallengeGroup last = group(3L);
        when(groupMapper.findGroupsToStart("RECRUITING", TODAY))
                .thenReturn(List.of(first, broken, last));
        when(transitionService.startOrCancel(any(ChallengeGroup.class))).thenReturn(true);
        when(transitionService.startOrCancel(broken))
                .thenThrow(new BusinessException("BOOM", "전이 실패"));

        assertEquals(2, batch().startDueGroups(TODAY), "실패한 한 건은 세지 않는다");

        verify(transitionService).startOrCancel(first);
        verify(transitionService).startOrCancel(broken);
        verify(transitionService).startOrCancel(last);
    }

    @Test
    @DisplayName("이미 전이된 그룹은 처리 건수에 세지 않는다")
    void countsOnlyActuallyTransitionedGroups() {
        ChallengeGroup changed = group(1L);
        ChallengeGroup alreadyDone = group(2L);
        when(groupMapper.findGroupsToStart("RECRUITING", TODAY))
                .thenReturn(List.of(changed, alreadyDone));
        when(transitionService.startOrCancel(changed)).thenReturn(true);
        when(transitionService.startOrCancel(alreadyDone)).thenReturn(false);

        assertEquals(1, batch().startDueGroups(TODAY));
    }

    @Test
    @DisplayName("재판 전이는 종료 다음다음 날부터 본다 — 기준일이 아니라 기준일 전날을 넘긴다")
    void judgeQueriesActiveGroupsEndedBeforeYesterday() {
        when(groupMapper.findGroupsToJudge("ACTIVE", TODAY.minusDays(1))).thenReturn(List.of());

        assertEquals(0, batch().judgeEndedGroups(TODAY));

        /*
         * 이 한 줄이 이 테스트의 전부다. TODAY 를 그대로 넘기면 종료 다음 날 자정에 JUDGING 이 되고,
         * 평가·기소 배치(#168)가 그날 봐야 할 그룹이 status 조건에서 빠져 마지막 날치 기소가 사라진다.
         */
        verify(groupMapper).findGroupsToJudge("ACTIVE", TODAY.minusDays(1));
        verify(groupMapper, never()).updateStatusIfCurrent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("ACTIVE 일 때만 JUDGING 으로 바꾼다 — 0 행이면 세지 않는다(멱등)")
    void judgeCountsOnlyRowsActuallyChanged() {
        when(groupMapper.findGroupsToJudge("ACTIVE", TODAY.minusDays(1)))
                .thenReturn(List.of(group(1L), group(2L)));
        when(groupMapper.updateStatusIfCurrent(1L, "ACTIVE", "JUDGING")).thenReturn(1);
        when(groupMapper.updateStatusIfCurrent(2L, "ACTIVE", "JUDGING")).thenReturn(0);

        assertEquals(1, batch().judgeEndedGroups(TODAY), "이미 전이된 그룹은 세지 않는다");
    }

    @Test
    @DisplayName("재판 전이도 한 그룹이 터지면 나머지를 계속 처리한다")
    void judgeOneGroupFailureDoesNotStopTheRest() {
        when(groupMapper.findGroupsToJudge("ACTIVE", TODAY.minusDays(1)))
                .thenReturn(List.of(group(1L), group(2L), group(3L)));
        when(groupMapper.updateStatusIfCurrent(anyLong(), anyString(), anyString())).thenReturn(1);
        when(groupMapper.updateStatusIfCurrent(2L, "ACTIVE", "JUDGING"))
                .thenThrow(new BusinessException("BOOM", "전이 실패"));

        assertEquals(2, batch().judgeEndedGroups(TODAY));

        verify(groupMapper).updateStatusIfCurrent(3L, "ACTIVE", "JUDGING");
    }

    @Test
    @DisplayName("재판 전이는 알림을 발행하지 않는다 — 전이 서비스를 거치지 않는다")
    void judgePublishesNoNotification() {
        when(groupMapper.findGroupsToJudge("ACTIVE", TODAY.minusDays(1))).thenReturn(List.of(group(1L)));
        when(groupMapper.updateStatusIfCurrent(1L, "ACTIVE", "JUDGING")).thenReturn(1);

        batch().judgeEndedGroups(TODAY);

        verifyNoInteractions(transitionService);
    }

    private ChallengeGroupStatusBatchService batch() {
        return new ChallengeGroupStatusBatchService(groupMapper, transitionService);
    }

    private ChallengeGroup group(long id) {
        return ChallengeGroup.builder()
                .id(id)
                .adminId(id * 10)
                .groupName("그룹 " + id)
                .startDate(TODAY)
                .endDate(TODAY.plusDays(2))
                .status("RECRUITING")
                .build();
    }
}

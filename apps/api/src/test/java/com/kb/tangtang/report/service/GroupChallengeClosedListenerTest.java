package com.kb.tangtang.report.service;

import com.kb.tangtang.challenge.domain.GroupChallengeEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 그룹 확정 → 리포트 그룹 전적 갱신 (이슈 #172 · #267).
 *
 * <p>지키는 것은 둘이다 — <b>기준 월을 종료일에서 뽑는가</b>, 그리고 <b>한 사람의 실패가
 * 나머지를 막지 않는가</b>.
 */
@ExtendWith(MockitoExtension.class)
class GroupChallengeClosedListenerTest {

    @Mock private ChallengeMonthlyReportSnapshotService snapshotService;

    @InjectMocks private GroupChallengeClosedListener listener;

    private static GroupChallengeEvents.GroupChallengeClosed closed(LocalDate endDate, Long... userIds) {
        return new GroupChallengeEvents.GroupChallengeClosed(1L, endDate, List.of(userIds));
    }

    /**
     * 말일에 끝난 그룹은 확정이 다음 달로 넘어가는 것이 정상 경로다. 수신 시각으로 월을 뽑으면
     * 8월 챌린지 전적이 9월 리포트에 붙어, 8월 판결문에는 영영 그룹 기록이 없다.
     */
    @Test
    @DisplayName("기준 월은 수신 시각이 아니라 챌린지 종료일에서 뽑는다")
    void usesEndDateMonthNotNow() {
        listener.onGroupChallengeClosed(closed(LocalDate.of(2026, 8, 31), 10L));

        verify(snapshotService).refreshUserMonthGroupRecord(10L, YearMonth.of(2026, 8));
    }

    @Test
    @DisplayName("참여자 전원의 그룹 전적을 갱신한다")
    void refreshesEveryMember() {
        listener.onGroupChallengeClosed(closed(LocalDate.of(2026, 8, 31), 10L, 20L, 30L));

        verify(snapshotService).refreshUserMonthGroupRecord(10L, YearMonth.of(2026, 8));
        verify(snapshotService).refreshUserMonthGroupRecord(20L, YearMonth.of(2026, 8));
        verify(snapshotService).refreshUserMonthGroupRecord(30L, YearMonth.of(2026, 8));
    }

    /**
     * 리스너는 {@code @Async} 라 예외가 튀어도 되돌릴 트랜잭션이 없다 — 그냥 나머지 사람이
     * 누락될 뿐이다. 확정 이벤트는 그룹당 한 번뿐이라 다시 받을 기회도 없다.
     */
    @Test
    @DisplayName("한 사람의 갱신 실패가 나머지를 막지 않는다")
    void oneFailureDoesNotStopTheRest() {
        doThrow(new IllegalStateException("DB 오류"))
                .when(snapshotService).refreshUserMonthGroupRecord(anyLong(), any());

        listener.onGroupChallengeClosed(closed(LocalDate.of(2026, 8, 31), 10L, 20L));

        verify(snapshotService).refreshUserMonthGroupRecord(20L, YearMonth.of(2026, 8));
    }
}

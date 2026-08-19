package com.kb.tangtang.report.service;

import com.kb.tangtang.challenge.domain.GroupChallengeEvents;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 그룹 챌린지가 확정되면 그 달 리포트의 그룹 전적을 곧바로 채운다 (이슈 #172 · #267).
 *
 * <p><b>보강 cron 만으로는 못 채우는 그룹이 있다.</b> 투표 마감이 기소 + 30시간이라 말일에 끝난
 * 그룹은 확정이 다음 달 2일 새벽을 넘기는 것이 정상 경로다. cron 창(익월 2일 00:40)을 지나서
 * 확정되면 그 그룹의 전적은 <b>영원히 채워지지 않는다</b> — 다음 달의 cron 은 그 다음 달을 본다.
 * 확정 시점에 바로 반응하는 이 리스너가 그 구멍을 막는다.
 *
 * <p>기준 월은 <b>수신 시각이 아니라 챌린지 종료일</b>에서 뽑는다. 위 이유로 확정이 다음 달로
 * 넘어가는 것이 정상이라, 수신 시각으로 월을 뽑으면 전적이 엉뚱한 달의 리포트에 붙는다.
 *
 * <p>{@code @Async} 다 — 리포트 갱신이 실패해도 챌린지 확정 트랜잭션은 이미 커밋됐고 되돌아가서도
 * 안 된다. 한 사람의 실패가 나머지를 막지 않도록 사용자마다 예외를 삼킨다.
 *
 * <p>대상 월의 리포트 행이 아직 없으면 {@code updateMonthlyGroupRecord} 가 0행으로 끝난다(무해).
 * 익월 1일 월 확정 배치가 스냅샷을 만들 때 그 안에서 같은 집계를 다시 한다.
 *
 * <p>{@code challenge} 가 {@code report} 서비스를 직접 부르지 않고 이벤트를 쓰는 이유는
 * {@code apps/api/AGENTS.md} 의 모듈 경계 규칙이다.
 */
@Component
@Log4j2
public class GroupChallengeClosedListener {

    private final ChallengeMonthlyReportSnapshotService snapshotService;

    public GroupChallengeClosedListener(ChallengeMonthlyReportSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Async
    @EventListener
    public void onGroupChallengeClosed(GroupChallengeEvents.GroupChallengeClosed event) {
        YearMonth targetMonth = YearMonth.from(event.getEndDate());
        for (Long userId : event.getMemberUserIds()) {
            try {
                snapshotService.refreshUserMonthGroupRecord(userId, targetMonth);
            } catch (RuntimeException exception) {
                log.error("그룹 전적 갱신 실패. userId={}, groupId={}, yearMonth={}",
                        userId, event.getGroupId(), targetMonth, exception);
            }
        }
        log.info("그룹 전적 갱신 완료. groupId={}, yearMonth={}, members={}",
                event.getGroupId(), targetMonth, event.getMemberUserIds().size());
    }
}

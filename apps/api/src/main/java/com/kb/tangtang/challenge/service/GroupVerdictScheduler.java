package com.kb.tangtang.challenge.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 개표 배치의 실행 주기만 담당한다 (이슈 #172).
 *
 * <p>판정 로직을 두지 않는다 — DEV 수동 트리거와 단위 테스트가
 * {@link GroupVerdictBatchService} 를 직접 부른다.
 * ({@link GroupTrialDeadlineScheduler} 와 같은 구조)
 *
 * <p><b>변론 마감 배치({@link GroupTrialDeadlineScheduler})에 붙이지 않았다.</b> 이쪽은 표가
 * 동률이면 판사 탕이(LLM)를 HTTP 로 부르느라 한 틱이 길어질 수 있는데, 같은 스케줄러에 있으면
 * 변론 마감({@code DEFENSE_WAIT → VOTING})이 그만큼 밀린다. 나눠 두면 서로 다른 스레드를 쓴다.
 *
 * <p><b>주기가 5분이 아니라 1분이다.</b> 「전원이 투표를 끝내면 마감 전이라도 확정한다」가
 * 이 배치의 핵심 동작인데(팀 결정 2026-08-18), 마지막 표를 던진 사람이 결과를 보기까지의 지연이
 * 그대로 주기다. 확정 대상이 없으면 SELECT 한 번으로 끝나 비용이 거의 없다.
 */
@Component
public class GroupVerdictScheduler {

    private final GroupVerdictBatchService batchService;

    public GroupVerdictScheduler(GroupVerdictBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelayString = "${challenge.trial.verdict.fixed-delay-ms}")
    public void confirmDueVerdicts() {
        batchService.confirmDueVerdicts();
    }
}

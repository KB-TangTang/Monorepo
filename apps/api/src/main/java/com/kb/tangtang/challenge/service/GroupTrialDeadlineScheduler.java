package com.kb.tangtang.challenge.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 변론 마감 배치의 실행 주기만 담당한다 (이슈 #170).
 *
 * <p>판정 로직을 두지 않는다 — DEV 수동 트리거와 단위 테스트가
 * {@link GroupTrialDeadlineBatchService} 를 직접 부른다.
 * ({@link GroupChallengeEvaluationScheduler} 와 같은 구조)
 *
 * <p><b>일일 cron({@link ChallengeGroupStatusScheduler}, 00:01)에 붙이지 않았다.</b> 변론 창이
 * 6시간인데 하루 한 번만 검사하면 마감이 지난 재판이 최대 24시간 멈춰 있다. 배심원은 투표 화면이
 * 열리기를 그만큼 기다리고, 투표 마감(기소 + 30시간)은 그대로 흘러 표를 받지 못한 채 개표된다.
 *
 * <p>{@code fixedRate} 가 아니라 {@code fixedDelay} 다 — 이전 실행이 끝난 뒤부터 시간을 재서
 * 느린 틱이 다음 틱과 겹치지 않는다. 겹쳐도 UPDATE 조건 덕에 결과는 같지만, 같은 행을 두 스레드가
 * 잠그면 대기가 길어진다.
 */
@Component
public class GroupTrialDeadlineScheduler {

    private final GroupTrialDeadlineBatchService batchService;

    public GroupTrialDeadlineScheduler(GroupTrialDeadlineBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelayString = "${challenge.trial.deadline.fixed-delay-ms}")
    public void closeExpiredDefenses() {
        batchService.closeExpiredDefenses();
    }
}

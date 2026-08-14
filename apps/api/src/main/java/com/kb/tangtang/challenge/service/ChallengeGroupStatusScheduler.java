package com.kb.tangtang.challenge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 그룹 챌린지 시작 전이 배치의 실행 시각만 담당한다 (이슈 #152).
 *
 * <p>판정 로직을 두지 않는다 — DEV 수동 트리거와 단위 테스트가 배치 서비스를 직접 부르기 때문에,
 * 여기에 로직이 있으면 그 경로들이 다른 코드를 타게 된다.
 * ({@code RelativeMissionAssignmentScheduler} 와 같은 구조)
 */
@Component
public class ChallengeGroupStatusScheduler {

    private final ChallengeGroupStatusBatchService batchService;
    private final ZoneId zoneId;

    public ChallengeGroupStatusScheduler(
            ChallengeGroupStatusBatchService batchService,
            @Value("${challenge.group.status-transition.zone}") String zoneId) {
        this.batchService = batchService;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${challenge.group.status-transition.cron}",
            zone = "${challenge.group.status-transition.zone}")
    public void startDueGroups() {
        /*
         * 기준일을 여기서 계산하므로 cron 을 00:00 정각으로 두면 안 된다 —
         * 정각에는 이 값이 전날로 나올 수 있어 그날 시작하는 그룹이 통째로 누락된다.
         * 기본값을 00:01 로 두는 이유이며 근거는 application.properties 주석에 적어 두었다.
         */
        batchService.startDueGroups(LocalDate.now(zoneId));
    }
}

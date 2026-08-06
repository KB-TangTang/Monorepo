package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 알림 처리 실패 재시도 배치 (NT_01_04).
 *
 * 브로커가 없는 구조라 DB 기반 재처리다 (DECISIONS.md 2026-07-26 Kafka 전면 제거).
 * tbl_notification_dlq 에는 다음 시각 컬럼이 없으므로 reg_date + 누적 백오프로 계산한다.
 */
@Component
public class NotificationDlqRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDlqRetryScheduler.class);

    /** 누적 분. retry_count 가 인덱스다. 5회에 도달하면 중단한다 */
    private static final long[] BACKOFF_MINUTES = {1, 6, 21, 81, 441};
    private static final int MAX_RETRY = 5;

    private final NotificationDlqMapper dlqMapper;
    private final Clock clock;

    /*
     * ⚠ 이 프로젝트에는 Clock 빈이 없다. AccountLinkService·LinkProgressStore 와 같은 방식으로
     *   기본 생성자가 systemDefaultZone() 을 넘긴다. 이렇게 하지 않으면 스프링이 Clock 을
     *   주입하지 못해 컨텍스트 기동이 실패한다 — 컴파일은 통과하므로 배포에서야 드러난다.
     */
    public NotificationDlqRetryScheduler(NotificationDlqMapper dlqMapper) {
        this(dlqMapper, Clock.systemDefaultZone());
    }

    public NotificationDlqRetryScheduler(NotificationDlqMapper dlqMapper, Clock clock) {
        this.dlqMapper = dlqMapper;
        this.clock = clock;
    }

    /** reg_date 기준 다음 재시도 시각. */
    public LocalDateTime nextAttemptAt(NotificationDlqRow row) {
        int index = Math.min(row.getRetryCount(), BACKOFF_MINUTES.length - 1);
        return row.getRegDate().plusMinutes(BACKOFF_MINUTES[index]);
    }

    @Scheduled(fixedDelay = 60_000)
    public void retryDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (NotificationDlqRow row : dlqMapper.findRetryable(MAX_RETRY)) {
            if (now.isBefore(nextAttemptAt(row))) {
                continue;
            }
            try {
                /* 실제 재처리 훅. 지금은 재시도 횟수만 올린다 —
                 * 알림 본문은 이미 tbl_notification 에 있어 화면에서 조회된다(NT_01_04). */
                dlqMapper.increaseRetry(row.getDlqId());
            } catch (Exception e) {
                log.error("DLQ 재시도 실패 dlqId={}", row.getDlqId(), e);
            }
        }
    }
}

package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * ⚠ 생성자가 둘이다. **어느 쪽을 쓸지 명시해 둔다** —
     *   인자 없는 생성자가 없는 상태에서 후보가 둘이면 Spring 이 고르지 못해 컨텍스트 로딩이 실패한다.
     *   컴파일도 되고 단위 테스트도 생성자를 직접 부르므로 통과한다 — 배포에서만 드러난다.
     *   LinkProgressStore·AccountLinkService 가 같은 이유로 @Autowired 를 달고 있다.
     */
    @Autowired
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

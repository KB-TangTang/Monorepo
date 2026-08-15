package com.kb.tangtang.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqPayload;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import com.kb.tangtang.notification.mapper.NotificationMapper;
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
 *
 * 재처리 결과에 따라 행을 이렇게 다룬다 (이슈 #69).
 * <ul>
 *   <li>성공 → 행 삭제. 남겨두면 5회를 채울 때까지 같은 알림을 계속 보낸다</li>
 *   <li>실패 → retry_count 증가. 백오프가 늘어나고 5회에서 조회 대상에서 빠진다</li>
 *   <li>다시 만들 수 없는 행(payload 손상·정보 부족) → retry_count 만 증가시켜 자연히 빠지게 둔다</li>
 * </ul>
 */
@Component
public class NotificationDlqRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDlqRetryScheduler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 누적 분. retry_count 가 인덱스다. 5회에 도달하면 중단한다 */
    private static final long[] BACKOFF_MINUTES = {1, 6, 21, 81, 441};
    private static final int MAX_RETRY = 5;

    private final NotificationDlqMapper dlqMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final NotificationSender sender;
    private final Clock clock;

    /*
     * ⚠ 생성자가 둘이다. **어느 쪽을 쓸지 명시해 둔다** —
     *   인자 없는 생성자가 없는 상태에서 후보가 둘이면 Spring 이 고르지 못해 컨텍스트 로딩이 실패한다.
     *   컴파일도 되고 단위 테스트도 생성자를 직접 부르므로 통과한다 — 배포에서만 드러난다.
     *   LinkProgressStore·AccountLinkService 가 같은 이유로 @Autowired 를 달고 있다.
     */
    @Autowired
    public NotificationDlqRetryScheduler(NotificationDlqMapper dlqMapper,
                                         NotificationMapper notificationMapper,
                                         NotificationService notificationService,
                                         NotificationSender sender) {
        this(dlqMapper, notificationMapper, notificationService, sender, Clock.systemDefaultZone());
    }

    public NotificationDlqRetryScheduler(NotificationDlqMapper dlqMapper,
                                         NotificationMapper notificationMapper,
                                         NotificationService notificationService,
                                         NotificationSender sender,
                                         Clock clock) {
        this.dlqMapper = dlqMapper;
        this.notificationMapper = notificationMapper;
        this.notificationService = notificationService;
        this.sender = sender;
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
                if (retry(row)) {
                    dlqMapper.delete(row.getDlqId());
                } else {
                    dlqMapper.increaseRetry(row.getDlqId());
                }
            } catch (Exception e) {
                /* 한 행 때문에 배치 전체가 멈추면 안 된다 */
                log.error("DLQ 재시도 실패 dlqId={}", row.getDlqId(), e);
                increaseQuietly(row.getDlqId());
            }
        }
    }

    /**
     * 한 행을 재처리한다.
     *
     * @return 이 행을 지워도 되면 true (성공했거나, 대상 알림이 이미 사라져 재시도가 무의미할 때)
     */
    private boolean retry(NotificationDlqRow row) {
        NotificationDlqPayload payload = parse(row.getPayloadJson());
        if (payload == null) {
            return false;
        }

        if (payload.pointsToSavedNotification()) {
            /* 저장은 됐고 푸시만 실패한 행. 본문을 지어내지 않고 DB 에서 읽어 다시 보낸다 */
            Notification saved = notificationMapper.findById(payload.notificationId(), payload.userId());
            if (saved == null) {
                /* 알림이 사라졌다(삭제·정리 등). 계속 재시도해도 살아나지 않는다 */
                log.warn("DLQ 대상 알림이 없어 행을 정리한다 dlqId={} notificationId={}",
                        row.getDlqId(), payload.notificationId());
                return true;
            }
            return sender.trySend(saved);
        }

        if (!payload.canRebuild()) {
            /* 문구 자체를 못 만들어 실패한 행 등. 재시도해도 같은 결과라 재시도 횟수만 올린다 */
            log.warn("DLQ 행을 다시 만들 정보가 없다 dlqId={} payload={}",
                    row.getDlqId(), row.getPayloadJson());
            return false;
        }

        NotificationType type = typeOf(payload.type());
        if (type == null) {
            log.warn("DLQ 행의 알림 종류를 알 수 없다 dlqId={} type={}", row.getDlqId(), payload.type());
            return false;
        }

        /*
         * PAYMENT_DUE 는 고정지출 항목·예정일·알림 유형 이력이 중복을 막는다.
         * 일반적인 안 읽음 중복 억제를 쓰면 이전 결제 주기의 안 읽음 알림 때문에
         * 새 예정일의 재시도가 사라질 수 있으므로, 원 요청과 같이 바로 저장한다.
         */
        if (type == NotificationType.PAYMENT_DUE) {
            return sender.trySend(notificationService.create(
                    payload.userId(), type, payload.content(), payload.deepLinkUrl()));
        }

        /*
         * 재시도 사이에 같은 알림이 이미 만들어졌을 수 있다(예: 즉시 조회를 다시 눌렀을 때).
         * 그때는 또 만들지 않고 행만 지운다 — 목적은 이미 달성됐다 (이슈 #70).
         */
        return notificationService
                .createUnlessDuplicate(payload.userId(), type, payload.content(), payload.deepLinkUrl())
                .map(sender::trySend)
                .orElse(true);
    }

    private NotificationDlqPayload parse(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(payloadJson, NotificationDlqPayload.class);
        } catch (Exception e) {
            /* 손상된 행 하나가 배치를 멈추게 두지 않는다 */
            log.warn("DLQ payload 를 읽지 못했다: {}", payloadJson);
            return null;
        }
    }

    private static NotificationType typeOf(String name) {
        try {
            return NotificationType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 재시도 처리 중 예외가 났을 때의 뒷정리. 여기서 또 터지면 배치가 멈춘다 */
    private void increaseQuietly(long dlqId) {
        try {
            dlqMapper.increaseRetry(dlqId);
        } catch (Exception e) {
            log.error("DLQ retry_count 증가 실패 dlqId={}", dlqId, e);
        }
    }
}

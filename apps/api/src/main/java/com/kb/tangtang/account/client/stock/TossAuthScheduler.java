package com.kb.tangtang.account.client.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 토스 액세스 토큰 주기 갱신.
 *
 * 토큰 유효기간은 86,400초(24시간, 토스 문서). ⚠ 예전엔 23시간 고정 주기로만 갱신했는데(QA
 * 지적사항), 만료 직전 갱신이 한 번이라도 실패하면(네트워크 순단 등) 다음 고정 주기까지 최대
 * 23시간 동안 토큰 없이 방치됐다. 그래서 지금은 **짧은 주기(application.properties 의
 * toss.auth.refresh-check-fixed-delay-ms, 기본 5분)로 자주 "갱신이 필요한가"만 확인**하고,
 * 실제로 필요할 때(토큰이 없거나 REFRESH_BEFORE_EXPIRY 안으로 만료가 다가왔을 때)만 토스를
 * 부른다 — 평소엔 대부분의 확인이 아무 일도 안 하고, 갱신이 실패해도 몇 분 뒤 다음 확인에서
 * 바로 재시도된다.
 *
 * ⚠ client 당 토큰이 1개뿐이고 재발급 즉시 이전 토큰이 무효화되므로(TossAuthClient 참고),
 *   토큰 갱신은 이 스케줄러 한 곳에서만 일어나야 한다.
 */
@Component
public class TossAuthScheduler {

    private static final Logger log = LoggerFactory.getLogger(TossAuthScheduler.class);

    /** 만료 이 시간 전부터 "갱신이 필요하다"고 본다. 만료 정각에 맞추면 그 순간 실패 시 바로 빈 토큰이 된다. */
    private static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofHours(1);

    private final TossAuthClient tossAuthClient;
    private final TossTokenHolder tokenHolder;

    public TossAuthScheduler(TossAuthClient tossAuthClient, TossTokenHolder tokenHolder) {
        this.tossAuthClient = tossAuthClient;
        this.tokenHolder = tokenHolder;
    }

    @Scheduled(fixedDelayString = "${toss.auth.refresh-check-fixed-delay-ms}")
    public void refresh() {
        if (!tokenHolder.needsRefresh(REFRESH_BEFORE_EXPIRY)) {
            return;
        }
        try {
            TossAccessToken token = tossAuthClient.fetchToken();
            tokenHolder.update(token.getAccessToken(), token.getExpiresInSeconds());
            log.info("토스 액세스 토큰 갱신 완료 expiresInSeconds={}", token.getExpiresInSeconds());
        } catch (Exception e) {
            /*
             * 갱신 실패로 스케줄러 자체가 죽으면 다음 확인 주기마저 사라진다. 이전 토큰이 아직
             * 만료 전이면 TossTokenHolder 가 계속 그 값을 돌려주므로, 여기서는 로그만 남기고
             * 다음 확인 주기(5분 뒤)에 재시도한다 — NotificationDlqRetryScheduler 와 같은 원칙.
             */
            log.error("토스 액세스 토큰 갱신 실패", e);
        }
    }
}

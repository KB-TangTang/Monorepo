package com.kb.tangtang.account.client.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 토스 액세스 토큰 주기 갱신.
 *
 * 토큰 유효기간은 86,400초(24시간, 토스 문서). 만료 정각에 맞추면 그 순간 갱신이 실패했을 때
 * 바로 빈 토큰이 되므로, 기본 주기(application.properties 의 toss.auth.refresh-fixed-delay-ms)를
 * 만료보다 짧게 잡아 여유를 둔다.
 *
 * ⚠ client 당 토큰이 1개뿐이고 재발급 즉시 이전 토큰이 무효화되므로(TossAuthClient 참고),
 *   토큰 갱신은 이 스케줄러 한 곳에서만 일어나야 한다.
 */
@Component
public class TossAuthScheduler {

    private static final Logger log = LoggerFactory.getLogger(TossAuthScheduler.class);

    private final TossAuthClient tossAuthClient;
    private final TossTokenHolder tokenHolder;

    public TossAuthScheduler(TossAuthClient tossAuthClient, TossTokenHolder tokenHolder) {
        this.tossAuthClient = tossAuthClient;
        this.tokenHolder = tokenHolder;
    }

    @Scheduled(fixedDelayString = "${toss.auth.refresh-fixed-delay-ms}")
    public void refresh() {
        try {
            TossAccessToken token = tossAuthClient.fetchToken();
            tokenHolder.update(token.getAccessToken(), token.getExpiresInSeconds());
            log.info("토스 액세스 토큰 갱신 완료 expiresInSeconds={}", token.getExpiresInSeconds());
        } catch (Exception e) {
            /*
             * 갱신 실패로 스케줄러 자체가 죽으면 다음 주기마저 사라진다. 이전 토큰이 아직
             * 만료 전이면 TossTokenHolder 가 계속 그 값을 돌려주므로, 여기서는 로그만 남기고
             * 다음 주기에 재시도한다 — NotificationDlqRetryScheduler 와 같은 원칙.
             */
            log.error("토스 액세스 토큰 갱신 실패", e);
        }
    }
}

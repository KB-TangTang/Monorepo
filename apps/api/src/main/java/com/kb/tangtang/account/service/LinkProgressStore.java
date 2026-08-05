package com.kb.tangtang.account.service;

import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 진행 상태 보관소. connectionId 로 찾고 TTL 이 지나면 버린다.
 *
 * 스케줄러를 두지 않고 **접근할 때마다 만료분을 정리한다.** 연결 시도는 사용자당 몇 건뿐이라
 * 별도 배치를 돌릴 이유가 없고, 스케줄러는 그 자체로 관리 대상이 된다.
 */
@Component
public class LinkProgressStore {

    /** 보관 시간. 인증 유효시간(5분)보다 넉넉히 잡아 조회 중 만료되지 않게 한다. */
    static final Duration TTL = Duration.ofMinutes(10);

    private final Map<String, LinkProgress> store = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * ⚠ 생성자가 둘이다. **어느 쪽을 쓸지 명시해 둔다** —
     * 지금은 인자 없는 생성자가 있어 Spring 이 그쪽으로 폴백하지만, 누가 이 생성자를 지우거나
     * 인자를 붙이는 순간 `NoSuchMethodException: <init>()` 로 컨텍스트 로딩이 실패한다.
     * `AccountLinkService` 에서 실제로 겪은 사고이고, 단위 테스트는 생성자를 직접 호출하므로 잡지 못한다.
     */
    @Autowired
    public LinkProgressStore() {
        this(Clock.systemDefaultZone());
    }

    /** 테스트에서 시간을 고정하기 위한 생성자. */
    public LinkProgressStore(Clock clock) {
        this.clock = clock;
    }

    public void put(LinkProgress progress) {
        evictExpired();
        store.put(progress.getConnectionId(), progress);
    }

    /**
     * 소유자 확인까지 함께 한다.
     * connectionId 만으로 찾으면 남의 연결 진행 상황을 들여다볼 수 있다.
     */
    public LinkProgress get(long userId, String connectionId) {
        evictExpired();
        LinkProgress progress = store.get(connectionId);
        if (progress == null || progress.getUserId() != userId) {
            throw new BusinessException("CONNECTION_NOT_FOUND",
                    "연결 정보를 찾을 수 없어요. 처음부터 다시 시도해 주세요.");
        }
        return progress;
    }

    public void remove(String connectionId) {
        store.remove(connectionId);
    }

    int size() {
        return store.size();
    }

    private void evictExpired() {
        Instant deadline = clock.instant().minus(TTL);
        Iterator<Map.Entry<String, LinkProgress>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().getCreatedAt().isBefore(deadline)) {
                iterator.remove();
            }
        }
    }
}

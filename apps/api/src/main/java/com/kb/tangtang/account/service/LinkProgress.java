package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.account.domain.ProgressStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 번의 연결 시도에 대한 진행 상태.
 *
 * **DB 테이블을 만들지 않는다.** 연동이 끝나면 버려지는 임시 데이터이고,
 * 팀 공용 schema.sql 변경은 협의·적용 절차가 필요해 이번 범위를 끌고 간다.
 * 단일 프로세스 구조(브로커 없음)라 메모리로 충분하다. (설계서 §7.3)
 *
 * 한계: 서버가 재시작되면 진행 중인 연동이 사라진다. 사용자는 처음부터 다시 한다.
 */
public class LinkProgress {

    private final long userId;
    private final String connectionId;
    private final Instant createdAt;

    /** 기관 코드 → 진행 상태. 선택한 순서를 유지해야 화면 목록이 흔들리지 않는다. */
    private final Map<String, ProgressStatus> statuses = new LinkedHashMap<>();

    /** 기관 코드 → 기관명. 화면에 이름을 보여주기 위해 함께 들고 있는다. */
    private final Map<String, String> names = new LinkedHashMap<>();

    /** 조회에 성공한 계좌. 계좌 선택 화면이 이 목록을 읽는다. */
    private final List<FinancialAccountDto> accounts = new ArrayList<>();

    /**
     * 자동 연동 미리보기 행(대출·페이머니·카드, #334)의 **음수 accountId → 원본 키**(#467).
     *
     * 미리보기 id 는 화면에 보여줄 때 `-1, -2, …` 로 매번 새로 매기는 임시값이라 DB 에 없다.
     * 사용자가 그 행의 체크를 풀면 link() 가 이 맵으로 원본 키(`MOCK-LOAN-{id}` 등)를 되찾아
     * 제외 행을 남긴다. 키 형식은 FinancialSyncServiceImpl 이 저장할 때 쓰는 것과 같아야 한다 —
     * 그래야 동기화가 `inactiveKeys` 로 같은 상품을 알아보고 건너뛴다.
     */
    private final Map<Long, PreviewEntry> previews = new LinkedHashMap<>();

    /** 제외 행을 만들 때 필요한 최소 정보. 키가 곧 동기화 쪽 자연키다. */
    public record PreviewEntry(String key, String bankCode, String bankName, String accountName,
                               String accountType) {
    }

    public synchronized void registerPreview(long accountId, PreviewEntry entry) {
        previews.put(accountId, entry);
    }

    /** 없으면 null — 모르는 id 는 link() 가 조용히 무시한다(화면이 지어낸 값일 수 있다). */
    public synchronized PreviewEntry previewOf(long accountId) {
        return previews.get(accountId);
    }

    public LinkProgress(long userId, String connectionId, Map<String, String> organizations,
                        Instant createdAt) {
        this.userId = userId;
        this.connectionId = connectionId;
        this.createdAt = createdAt;
        organizations.forEach((code, name) -> {
            statuses.put(code, ProgressStatus.WAITING);
            names.put(code, name);
        });
    }

    public long getUserId() {
        return userId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized void mark(String organization, ProgressStatus status) {
        if (statuses.containsKey(organization)) {
            statuses.put(organization, status);
        }
    }

    public synchronized void addAccounts(List<FinancialAccountDto> found) {
        accounts.addAll(found);
    }

    public synchronized List<FinancialAccountDto> getAccounts() {
        return List.copyOf(accounts);
    }

    public synchronized Map<String, ProgressStatus> getStatuses() {
        return new LinkedHashMap<>(statuses);
    }

    public synchronized String nameOf(String organization) {
        return names.get(organization);
    }

    /** 다음에 조회할 기관. 없으면 null — 전부 끝났다는 뜻이다. */
    public synchronized String nextWaiting() {
        for (Map.Entry<String, ProgressStatus> entry : statuses.entrySet()) {
            if (entry.getValue() == ProgressStatus.WAITING) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 전부 끝났는지. 실패도 '끝난 것'으로 센다 —
     * 한 기관이 실패했다고 진행이 멈춰 있으면 화면이 죽은 것처럼 보인다.
     */
    public synchronized boolean isDone() {
        /*
         * ⚠ 빈 목록에서 allMatch 는 true 다. 그대로 두면 조회할 기관이 하나도 없을 때
         * `done=true` 인데 `percent=0` 인 모순 응답이 나가고, 화면은 빈 계좌 목록으로 넘어간다.
         */
        if (statuses.isEmpty()) {
            return false;
        }
        return statuses.values().stream()
                .allMatch(status -> status == ProgressStatus.DONE || status == ProgressStatus.FAILED);
    }

    public synchronized int percent() {
        if (statuses.isEmpty()) {
            return 0;
        }
        long finished = statuses.values().stream()
                .filter(status -> status == ProgressStatus.DONE || status == ProgressStatus.FAILED)
                .count();
        return (int) (finished * 100 / statuses.size());
    }
}

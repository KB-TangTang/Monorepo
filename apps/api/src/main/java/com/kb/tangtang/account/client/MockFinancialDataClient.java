package com.kb.tangtang.account.client;

import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.account.client.sync.ScenarioKeyProvider;
import com.kb.tangtang.account.domain.AuthMethod;
import com.kb.tangtang.account.domain.AuthStatus;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 목서버 기반 구현. 개발·시연영상 본편이 이 구현으로 돈다.
 *
 * 목서버에는 **인증 API 가 없다** — 엔드포인트 6종이 전부 조회이고 사용자 식별은 scenarioKey 하나다
 * (2026-08-05 실측). 그래서 인증은 여기서 흉내낸다:
 *   · createConnection : 식별자를 발급하고 승인 예정 시각을 기록한다
 *   · getAuthStatus    : 승인 지연이 지나면 APPROVED 로 바뀐다
 * 흉내내는 범위는 **인증뿐**이고 계좌 데이터는 실제로 목서버를 호출해 가져온다.
 * 정적 화면이 아니라 진짜 통신이 일어나야 연동 과정이 시연에서 사실대로 보인다.
 *
 * 상태를 메모리에 두는 이유는 설계서 §7.3 과 같다 — 연동이 끝나면 버려지는 임시 데이터라
 * 팀 공용 schema.sql 을 건드릴 이유가 없다.
 */
public class MockFinancialDataClient implements FinancialDataClient {

    /** 간편인증 승인까지의 지연. 시연에서 기다릴 만하면서 대기 화면이 보이는 길이. */
    private static final long APPROVE_DELAY_SECONDS = 3;

    /** 인증 유효 시간. 화면의 남은 시간 표시와 같은 값이다. */
    private static final int EXPIRES_IN_SECONDS = 300;

    /**
     * 흉내내는 간편인증 수단. 실 CODEF 로 바뀌면 이 목록은 쓰이지 않는다.
     * 실제 마이데이터에서 흔히 제공하는 3종에 맞췄다.
     */
    private static final List<String> PROVIDERS = List.of("KAKAO", "PASS", "NAVER");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    /**
     * 시나리오 키는 사용자별로 다르게 정해진다(#334) — FinancialSyncServiceImpl 의 동기화가 쓰는
     * 것과 **같은** ScenarioKeyProvider 를 쓴다. 예전엔 이 클래스가 `financial.mock.scenario-key`
     * 라는 별도 프로퍼티로 고정된 시나리오 하나만 썼는데, 동기화 쪽은 `mock.server.scenario-keys`
     * 로 사용자마다 다른 시나리오를 골랐다 — 두 설정이 서로 다른 기본값(demo-normal-user vs 1)을
     * 가리켜, 같은 실사용자인데도 **계좌 연동 화면에서 고른 계좌**와 **연동 직후 배치가 동기화한
     * 계좌**가 서로 다른 시나리오(=다른 가짜 은행 데이터)에서 와 함께 쌓이는 사고가 났다.
     */
    private final ScenarioKeyProvider scenarioKeyProvider;
    private final Clock clock;

    /** 승인 기록 보관 기한. 인증 유효시간(5분)에 조회 시간을 얹은 값이다. */
    private static final long APPROVAL_TTL_SECONDS = 30 * 60L;

    /**
     * connectionId → 승인 예정 시각.
     *
     * ⚠ 예전 주석은 "TTL 은 AccountLinkService 의 진행 상태와 함께 정리된다" 고 했는데 **사실이 아니었다** —
     *   이 빈은 싱글턴이고 아무도 지우지 않아 연결 시도마다 엔트리가 영구히 쌓였다(2026-08-05 리뷰 지적).
     *   새 연결을 만들 때 지난 것을 함께 치운다.
     */
    private final Map<String, Instant> approvals = new ConcurrentHashMap<>();

    public MockFinancialDataClient(RestTemplate restTemplate, String baseUrl,
                                   ScenarioKeyProvider scenarioKeyProvider, Clock clock) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.scenarioKeyProvider = scenarioKeyProvider;
        this.clock = clock;
    }

    @Override
    public List<AuthMethod> supportedAuthMethods() {
        /* 목서버에는 기관 로그인 API 가 없다. 흉내낼 근거가 없으므로 간편인증만 내려준다. */
        return List.of(AuthMethod.SIMPLE_AUTH);
    }

    @Override
    public List<String> simpleAuthProviders() {
        return PROVIDERS;
    }

    @Override
    public ConnectionResult createConnection(ConnectionRequest request) {
        List<String> organizations = request.getOrganizations() == null
                ? List.of()
                : request.getOrganizations();
        if (organizations.isEmpty()) {
            throw new BusinessException("EXTERNAL_API_ERROR", "연결할 금융기관이 없어요.");
        }

        String connectionId = "mock-" + UUID.randomUUID();
        Instant deadline = clock.instant().minusSeconds(APPROVAL_TTL_SECONDS);
        approvals.values().removeIf(approveAt -> approveAt.isBefore(deadline));
        approvals.put(connectionId, clock.instant().plusSeconds(APPROVE_DELAY_SECONDS));

        return ConnectionResult.builder()
                .connectionId(connectionId)
                .status(AuthStatus.PENDING)
                .expiresInSeconds(EXPIRES_IN_SECONDS)
                .needsExtraAuth(false)
                .extraAuthType(null)
                .successOrganizations(List.copyOf(organizations))
                .failedOrganizations(List.of())
                .build();
    }

    @Override
    public AuthStatus getAuthStatus(String connectionId) {
        Instant approveAt = approvals.get(connectionId);
        if (approveAt == null) {
            throw new BusinessException("CONNECTION_NOT_FOUND",
                    "연결 정보를 찾을 수 없어요. 처음부터 다시 시도해 주세요.");
        }
        return clock.instant().isBefore(approveAt) ? AuthStatus.PENDING : AuthStatus.APPROVED;
    }

    @Override
    public AuthStatus submitExtraAuth(String connectionId, String authCode) {
        /* 목서버에는 2-way 추가인증이 없다. 요청이 오면 이미 승인된 것으로 본다. */
        getAuthStatus(connectionId);
        approvals.put(connectionId, clock.instant());
        return AuthStatus.APPROVED;
    }

    @Override
    public List<FinancialAccountDto> fetchAccounts(long userId, String connectionId, String organization) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/assets/accounts")
                .queryParam("scenarioKey", scenarioKeyProvider.resolve(userId))
                .toUriString();

        Map<String, Object> envelope;
        try {
            envelope = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            /* 목서버가 죽었거나 네트워크가 끊긴 경우. 기관 하나의 실패로 취급된다. */
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE",
                    "금융기관이 점검 중이에요. 잠시 후 다시 시도해 주세요.");
        }
        if (envelope == null || !(envelope.get("data") instanceof Map)) {
            throw new BusinessException("EXTERNAL_API_ERROR", "금융기관 연동에 실패했어요.");
        }

        Object accounts = ((Map<?, ?>) envelope.get("data")).get("accounts");
        if (!(accounts instanceof List)) {
            return List.of();
        }

        List<FinancialAccountDto> result = new ArrayList<>();
        for (Object item : (List<?>) accounts) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) item;
            String bankCode = firstNonBlank(row, "institutionCode", "bankCode");
            /*
             * 목서버는 기관별 필터를 지원하지 않는다(scenarioKey 하나로 전체를 준다).
             * 우리 쪽에서 걸러야 기관별 진행 표시가 실제 데이터와 맞는다.
             */
            if (organization != null && !organization.equals(bankCode)) {
                continue;
            }
            result.add(FinancialAccountDto.builder()
                    .organization(bankCode)
                    .bankName(firstNonBlank(row, "institutionName", "bankName"))
                    .accountName(firstNonBlank(row, "productName", "accountName"))
                    /*
                     * 목서버는 마스킹된 값을 준다. 실 CODEF 는 전체 계좌번호를 주므로
                     * 마스킹·암호화 책임은 어느 쪽이든 우리(AccountLinkService)에게 있다.
                     */
                    .accountNo(text(row.get("accountNoMasked")))
                    /*
                     * 구버전 목서버는 계좌 종류를 DEMAND_DEPOSIT / SAVINGS 로 판정해 내려줬다.
                     * v2 는 그 필드를 없애고 accountTypeCode('1001') 로 바꿨는데, 이건 우리 도메인 값이 아니라
                     * 그대로 넘기면 안 된다. 다만 v2 의 예적금 코드(2001/2002)는
                     * deposit_account 를 함께 내려줄 때 예적금 판정에 필요한 짧은 코드이므로
                     * 아래에서 숫자 코드로만 안전하게 보존한다.
                     *
                     * CODEF 의 숫자 코드(resAccountDeposit)가 아니므로 depositTypeCode 에 넣으면 안 된다
                     * — 그 컬럼은 VARCHAR(5) 라 저장이 깨진다.
                     */
                    .accountType(domainAccountType(row))
                    .depositTypeCode(mockDepositTypeCode(row))
                    .currency(text(row.get("currency")))
                    .balance(decimal(row.get("balance")))
                    .build());
        }
        result.sort(Comparator.comparing(FinancialAccountDto::getAccountName,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 목서버 응답에서 값을 꺼낸다. 앞 키부터 보고 비어 있으면 다음 키로 넘어간다.
     *
     * 목서버가 2026-08-06 v2 리팩토링에서 필드명을 바꿨다
     * (`bankCode`→`institutionCode`, `bankName`→`institutionName`, `accountName`→`productName`).
     * 목서버는 별도 레포·별도 배포라 두 형태가 잠시 공존할 수 있어 둘 다 읽는다.
     */
    private static String firstNonBlank(Map<?, ?> row, String... keys) {
        for (String key : keys) {
            String value = text(row.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 우리 도메인의 계좌 종류(`DEMAND_DEPOSIT`/`SAVINGS`)만 돌려준다.
     *
     * 구버전 목서버는 이 값을 그대로 줬다. v2 의 `accountTypeCode`('1001') 는 목서버 내부 코드라
     * 의미를 알 수 없으므로 넘기지 않고 null 로 둔다 — 호출부가 그때 판정한다.
     */
    private static String domainAccountType(Map<?, ?> row) {
        String given = text(row.get("accountType"));
        return "DEMAND_DEPOSIT".equals(given) || "SAVINGS".equals(given) ? given : null;
    }

    /** 목서버 v2 예적금 코드만 CODEF 호환 필드에 보존한다. 은행·증권 내부 코드는 제외한다. */
    private static String mockDepositTypeCode(Map<?, ?> row) {
        String code = text(row.get("accountTypeCode"));
        return code != null && code.matches("2\\d{3}") ? code : null;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

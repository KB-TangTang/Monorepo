package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.FinancialDataClient;
import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.CredentialDto;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.account.client.dto.IdentityDto;
import com.kb.tangtang.account.domain.AuthMethod;
import com.kb.tangtang.account.domain.AuthStatus;
import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.account.domain.ProgressStatus;
import com.kb.tangtang.account.domain.SyncStatus;
import com.kb.tangtang.account.dto.*;
import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.service.ConsentService;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 계좌 연동 흐름 조립 (이슈 #12).
 *
 * 화면 순서와 같다: 기관 선택 → 인증 → 조회 진행 → 계좌 선택 → 완료.
 * **어느 금융 데이터 공급자가 붙었는지 이 클래스는 모른다** — FinancialDataClient 뒤에 가려져 있다.
 *
 * 트랜잭션 경계는 여기다. 외부 API 호출 구간에는 트랜잭션을 걸지 않는다 —
 * 기관 조회가 수십 초 걸릴 수 있어 커넥션을 그동안 붙잡고 있으면 풀이 마른다.
 */
@Service
public class AccountLinkService {

    /** 마이데이터 재동의 주기. 1년마다 다시 연결해야 한다. */
    private static final int CONSENT_VALID_DAYS = 365;

    /** 즉시 조회 쿨다운. 과도한 반복 호출을 막는다 (AC_02_03). */
    private static final int REFRESH_COOLDOWN_SECONDS = 150;

    private final FinancialDataClient client;
    private final ConnectedAccountMapper mapper;
    private final LinkProgressStore progressStore;
    private final InstitutionCatalog catalog;
    private final AccountNumberPolicy accountNumbers;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    /**
     * 자동 동기화 배치의 실행 간격(밀리초). FinancialSyncBatchScheduler 의 fixedDelay 와 같은 값을
     * 읽는다 — 다음 자동 동기화 시각 안내를 여기서 따로 하드코딩하면(예전 "매일 18시") 배치 주기를
     * 바꾸는 순간 안내만 거짓말이 된다(이슈 #199 최종 리뷰).
     */
    private final long batchFixedDelayMs;
    /*
     * 제3자 제공(THIRD_PARTY) 동의 검사용.
     * ⚠ 화면 순서만으로는 못 막는다 — 계좌 연동 진입점이 셋이고 우회가 가능하다.
     *   동의는 화면 규칙이 아니라 서버 규칙이어야 한다. (DECISIONS.md 2026-08-11 (7))
     * ConsentService 는 의존이 가벼워 좁은 창구를 따로 만들지 않고 직접 주입한다
     * (반대 방향인 AuthService→계좌 조회는 AccountLinkService 가 무거워 ConnectedAccountQuery 를 뒀다).
     */
    private final ConsentService consentService;

    /**
     * ⚠ 생성자가 둘이라 **@Autowired 로 어느 쪽을 쓸지 명시해야 한다.**
     * 없으면 Spring 이 기본 생성자를 찾다가 `NoSuchMethodException: <init>()` 로 컨텍스트 로딩이 실패한다.
     * (2026-08-05 실제 발생 — 단위 테스트는 생성자를 직접 호출하므로 이 결함을 잡지 못한다)
     */
    @Autowired
    public AccountLinkService(FinancialDataClient client,
                              ConnectedAccountMapper mapper,
                              LinkProgressStore progressStore,
                              InstitutionCatalog catalog,
                              AccountNumberPolicy accountNumbers,
                              ApplicationEventPublisher events,
                              ConsentService consentService,
                              @Value("${financial.sync.batch.fixed-delay-ms}") long batchFixedDelayMs) {
        this(client, mapper, progressStore, catalog, accountNumbers, events, consentService,
                batchFixedDelayMs, Clock.systemDefaultZone());
    }

    /** 테스트에서 시간을 고정하기 위한 생성자. */
    AccountLinkService(FinancialDataClient client,
                       ConnectedAccountMapper mapper,
                       LinkProgressStore progressStore,
                       InstitutionCatalog catalog,
                       AccountNumberPolicy accountNumbers,
                       ApplicationEventPublisher events,
                       ConsentService consentService,
                       long batchFixedDelayMs,
                       Clock clock) {
        this.client = client;
        this.mapper = mapper;
        this.progressStore = progressStore;
        this.catalog = catalog;
        this.accountNumbers = accountNumbers;
        this.events = events;
        this.consentService = consentService;
        this.batchFixedDelayMs = batchFixedDelayMs;
        this.clock = clock;
    }

    /* ── 1단계: 기관 선택 ───────────────────────────────── */

    @Transactional(readOnly = true)
    public InstitutionListDto institutions(long userId) {
        List<String> connected = mapper.findActiveByUser(userId).stream()
                .map(ConnectedAccount::getBankCode)
                .distinct()
                .toList();
        /* 공급자가 다루지 못하는 기관은 아예 내려보내지 않는다 — 인증 수단과 같은 원칙이다. */
        Set<String> supported = client.supportedOrganizations();
        /*
         * 대출·페이머니도 같은 필터를 탄다. 실 CODEF 모드에서는 supportedOrganizations() 가
         * 은행 20곳만 돌려주므로 두 업권이 자동으로 빈 배열이 된다 — 의도한 동작이다.
         * 목 모드(제한 없음)에서만 카탈로그 전체가 그대로 내려간다.
         */
        return InstitutionListDto.builder()
                .banks(onlySupported(catalog.banks(connected), supported))
                .cards(onlySupported(catalog.cards(connected), supported))
                .securities(onlySupported(catalog.securities(connected), supported))
                .loans(onlySupported(catalog.loans(connected), supported))
                .payMoney(onlySupported(catalog.payMoney(connected), supported))
                .build();
    }

    /**
     * 계좌 연동 전 제3자 제공(THIRD_PARTY) 동의를 확인한다.
     *
     * ⚠ 화면 순서에만 맡기면 우회된다 — 계좌 연동 진입점이 셋(연결 계좌 관리 · 개인챌린지 CTA ·
     *   재연동)이고 전부 동의 화면을 거치지 않는 경로였다. 실제로 2026-08-11 까지
     *   **아무도 제3자 제공 동의를 하지 않은 채 계좌를 연동해 왔다.**
     *   마이데이터 동의는 화면 규칙이 아니라 서버 규칙이어야 한다. (DECISIONS.md 2026-08-11 (7))
     */
    private void requireFinancialConsent(long userId) {
        if (consentService.needsConsent(userId, ConsentScope.FINANCIAL)) {
            throw new BusinessException("CONSENT_REQUIRED",
                    "계좌를 연결하려면 제3자 제공 동의가 필요해요.");
        }
    }

    /** 빈 집합은 "제한 없음" 이다 (목서버). 그때는 목록을 그대로 둔다. */
    private static List<InstitutionDto> onlySupported(List<InstitutionDto> items,
                                                      Set<String> supported) {
        if (supported.isEmpty()) {
            return items;
        }
        return items.stream().filter(item -> supported.contains(item.getCode())).toList();
    }

    /* ── 2단계: 인증 ────────────────────────────────────── */

    /**
     * 지원 인증 수단. **프론트 분기의 유일한 근거다.**
     * 여기서 mock/codef 를 노출하면 "설정 교체만으로 전환" 이 거짓이 된다.
     */
    public AuthMethodListDto authMethods() {
        List<AuthMethodDto> methods = new ArrayList<>();
        for (AuthMethod method : client.supportedAuthMethods()) {
            List<ProviderDto> providers = method == AuthMethod.SIMPLE_AUTH
                    ? client.simpleAuthProviders().stream()
                            .map(code -> ProviderDto.builder()
                                    .code(code)
                                    .name(providerName(code))
                                    .build())
                            .toList()
                    : List.of();
            methods.add(AuthMethodDto.builder()
                    .type(method.name())
                    .providers(providers)
                    /* 개인정보 전송 여부는 공급자가 정한다. 프론트가 판단하지 않는다. */
                    .requiresIdentity(method == AuthMethod.SIMPLE_AUTH && client.requiresIdentity())
                    .build());
        }
        return AuthMethodListDto.builder().methods(methods).build();
    }

    /** 간편인증 요청. 선택한 기관을 한 번에 처리한다. */
    public ConnectionResponseDto requestSimpleAuth(long userId, SimpleAuthRequestDto request) {
        requireFinancialConsent(userId);
        if (request.getProvider() == null || request.getProvider().isBlank()) {
            throw new BusinessException("INVALID_CREDENTIALS", "인증 수단을 선택해 주세요.");
        }
        List<String> organizations = request.getOrganizations() == null
                ? List.of()
                : request.getOrganizations();
        requireOrganizations(organizations);

        ConnectionResult result = client.createConnection(ConnectionRequest.builder()
                .authMethod(AuthMethod.SIMPLE_AUTH)
                .provider(request.getProvider())
                .organizations(organizations)
                .credentials(List.of())
                /*
                 * 본인 정보는 **전달만 한다.** DB·로그 어디에도 남기지 않는다.
                 * 목 모드에서는 프론트가 보내지 않아 전부 null 이다.
                 */
                .identity(IdentityDto.builder()
                        .userName(request.getUserName())
                        .birthDate(request.getBirthDate())
                        .carrier(request.getCarrier())
                        .phoneNo(request.getPhoneNo())
                        .build())
                .build());

        rememberProgress(userId, result, organizations);
        return toResponse(result);
    }

    /**
     * 기관 로그인 인증.
     * ⚠ 자격증명은 이 메서드 안에서만 산다. 진행 상태·로그·DB 어디에도 남기지 않는다.
     */
    public ConnectionResponseDto requestInstitutionLogin(long userId,
                                                         InstitutionLoginRequestDto request) {
        requireFinancialConsent(userId);
        List<CredentialRequestDto> rows = request.getCredentials();
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("INVALID_CREDENTIALS", "금융기관 로그인 정보가 필요해요.");
        }
        for (CredentialRequestDto row : rows) {
            if (row.getId() == null || row.getId().isBlank()
                    || row.getPassword() == null || row.getPassword().isBlank()) {
                throw new BusinessException("INVALID_CREDENTIALS",
                        "아이디와 비밀번호를 모두 입력해 주세요.");
            }
        }

        List<String> organizations = rows.stream()
                .map(CredentialRequestDto::getOrganization)
                .toList();
        requireOrganizations(organizations);

        ConnectionResult result = client.createConnection(ConnectionRequest.builder()
                .authMethod(AuthMethod.INSTITUTION_LOGIN)
                .organizations(organizations)
                .credentials(rows.stream()
                        .map(row -> CredentialDto.builder()
                                .organization(row.getOrganization())
                                .loginType(row.getLoginType())
                                .id(row.getId())
                                .password(row.getPassword())
                                .build())
                        .toList())
                .build());

        rememberProgress(userId, result, organizations);
        return toResponse(result);
    }

    public AuthStatusDto authStatus(long userId, String connectionId) {
        progressStore.get(userId, connectionId);
        return AuthStatusDto.builder().status(client.getAuthStatus(connectionId).name()).build();
    }

    public ConnectionResponseDto submitExtraAuth(long userId, String connectionId,
                                                 ExtraAuthRequestDto request) {
        progressStore.get(userId, connectionId);
        if (request.getAuthCode() == null || request.getAuthCode().isBlank()) {
            throw new BusinessException("INVALID_CREDENTIALS", "인증번호를 입력해 주세요.");
        }
        AuthStatus status = client.submitExtraAuth(connectionId, request.getAuthCode());
        return ConnectionResponseDto.builder()
                .connectionId(connectionId)
                .status(status.name())
                .expiresInSeconds(0)
                .needsExtraAuth(false)
                .extraAuthType(null)
                .build();
    }

    /* ── 3단계: 조회 진행 ───────────────────────────────── */

    /**
     * 진행 상황 조회. **폴링될 때마다 기관을 하나씩 실제로 조회한다.**
     *
     * 별도 스레드로 미리 돌리지 않는 이유는 두 가지다.
     *   · 브로커도 비동기 인프라도 없는 단일 프로세스 구조다
     *   · 폴링 주기(1초)와 조회 속도가 자연스럽게 맞물려 화면 진행이 매끄럽다
     * 한 기관이 실패해도 FAILED 로 두고 다음 기관으로 넘어간다.
     */
    public LinkProgressDto progress(long userId, String connectionId) {
        LinkProgress progress = progressStore.get(userId, connectionId);

        if (client.getAuthStatus(connectionId) != AuthStatus.APPROVED) {
            /* 아직 승인 전이면 조회를 시작하지 않는다. */
            return toProgressDto(progress);
        }

        String next = progress.nextWaiting();
        if (next != null) {
            progress.mark(next, ProgressStatus.FETCHING);
            try {
                List<FinancialAccountDto> accounts = client.fetchAccounts(connectionId, next);
                progress.addAccounts(accounts);
                progress.mark(next, ProgressStatus.DONE);
            } catch (RuntimeException e) {
                /* 이 기관만 실패로 두고 나머지는 계속 진행한다. */
                progress.mark(next, ProgressStatus.FAILED);
            }
        }
        return toProgressDto(progress);
    }

    /* ── 4단계: 계좌 선택 ───────────────────────────────── */

    @Transactional(readOnly = true)
    public List<LinkableGroupDto> linkableAccounts(long userId, String connectionId) {
        LinkProgress progress = progressStore.get(userId, connectionId);
        List<String> linkedHashes = mapper.findActiveHashes(userId);

        Map<String, List<LinkableAccountDto>> grouped = new LinkedHashMap<>();
        List<FinancialAccountDto> accounts = progress.getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            FinancialAccountDto account = accounts.get(i);
            String hash = accountNumbers.hash(account.getOrganization(), account.getAccountNo());
            grouped.computeIfAbsent(account.getOrganization(), key -> new ArrayList<>())
                    .add(LinkableAccountDto.builder()
                            /* 아직 DB 에 없으므로 목록 순번을 임시 식별자로 쓴다. */
                            .accountId(i + 1L)
                            .bankCode(account.getOrganization())
                            .bankName(bankName(account))
                            .accountType(accountType(account))
                            .accountName(account.getAccountName())
                            .accountNoMasked(accountNumbers.mask(account.getAccountNo()))
                            .currency(account.getCurrency() == null ? "KRW" : account.getCurrency())
                            .balance(account.getBalance())
                            .alreadyLinked(linkedHashes.contains(hash))
                            .build());
        }

        return grouped.entrySet().stream()
                .map(entry -> LinkableGroupDto.builder()
                        .bankCode(entry.getKey())
                        .bankName(catalog.nameOf(entry.getKey()))
                        .shortLabel(catalog.shortLabelOf(entry.getKey()))
                        .accounts(entry.getValue())
                        .build())
                .toList();
    }

    /* ── 5단계: 연결 저장 ───────────────────────────────── */

    @Transactional
    public LinkResultDto link(long userId, LinkRequestDto request) {
        LinkProgress progress = progressStore.get(userId, request.getConnectionId());
        List<Long> requested = request.getAccountIds();
        if (requested == null || requested.isEmpty()) {
            throw new BusinessException("EXTERNAL_API_ERROR", "연결할 계좌를 선택해 주세요.");
        }

        List<FinancialAccountDto> accounts = progress.getAccounts();
        /*
         * 같은 계좌를 두 번 세지 않는다.
         * 중복이 그대로 들어오면 `reactivate` 가 두 번 matched 를 돌려줘 **1개를 연결하고 "2개 연결됨"**
         * 을 응답한다(MySQL 기본값이 CLIENT_FOUND_ROWS 라 두 번째도 1이 나온다).
         */
        Set<Long> ids = new LinkedHashSet<>(requested);
        /* 이번 호출에서 이미 처리한 해시. `existing` 은 루프 밖에서 한 번 읽은 스냅샷이라 여기서 보완한다. */
        Set<String> seen = new HashSet<>(mapper.findActiveHashes(userId));
        LocalDateTime now = LocalDateTime.now(clock);
        int linked = 0;

        for (Long id : ids) {
            int index = id.intValue() - 1;
            if (index < 0 || index >= accounts.size()) {
                continue;
            }
            FinancialAccountDto account = accounts.get(index);
            String hash = accountNumbers.hash(account.getOrganization(), account.getAccountNo());
            if (!seen.add(hash)) {
                /* 이미 연결됐거나 이번 요청에 또 실린 계좌는 조용히 건너뛴다. */
                continue;
            }

            ConnectedAccount row = ConnectedAccount.builder()
                    .userId(userId)
                    .codefConnectedId(progress.getConnectionId())
                    .bankCode(account.getOrganization())
                    .bankName(bankName(account))
                    .accountName(account.getAccountName())
                    .accountNoEncrypted(hash)
                    .accountNoMasked(accountNumbers.mask(account.getAccountNo()))
                    .accountType(accountType(account))
                    .depositTypeCode(account.getDepositTypeCode())
                    .balance(account.getBalance() == null ? BigDecimal.ZERO : account.getBalance())
                    .syncStatus(SyncStatus.NORMAL.name())
                    .lastSyncAt(now)
                    .expiresAt(now.plusDays(CONSENT_VALID_DAYS))
                    .build();

            /* 해제했던 계좌면 새 행을 만들지 않고 되살린다 (UNIQUE KEY 충돌 방지). */
            if (mapper.reactivate(row) == 0) {
                mapper.insert(row);
            }
            linked++;
        }

        if (linked == 0) {
            throw new BusinessException("EXTERNAL_API_ERROR", "연결된 계좌가 없어요.");
        }
        return LinkResultDto.builder().linkedCount(linked).build();
    }

    /* ── 연결 계좌 관리 ─────────────────────────────────── */

    @Transactional(readOnly = true)
    public ConnectedAccountListDto connectedAccounts(long userId) {
        List<ConnectedAccountDto> accounts = mapper.findActiveByUser(userId).stream()
                .map(row -> ConnectedAccountDto.builder()
                        .accountId(row.getId())
                        .bankCode(row.getBankCode())
                        .bankName(row.getBankName())
                        .shortLabel(catalog.shortLabelOf(row.getBankCode()))
                        .accountName(row.getAccountName())
                        .accountNoMasked(row.getAccountNoMasked())
                        .accountType(row.getAccountType())
                        .balance(row.getBalance())
                        .syncStatus(row.getSyncStatus())
                        .lastSyncAt(text(row.getLastSyncAt()))
                        .syncFailReason(row.getSyncFailReason())
                        .expiresAt(text(row.getExpiresAt()))
                        .build())
                .toList();

        /*
         * 자동 동기화는 FinancialSyncBatchScheduler 가 fixedDelay 간격(financial.sync.batch.fixed-delay-ms,
         * 현재 30분)으로 돌린다. 예전에는 여기서 "매일 18시" 를 하드코딩했는데 그런 배치는 존재한 적이
         * 없어 안내가 통째로 거짓이었다(이슈 #199 최종 리뷰). 정확한 다음 틱 시각은 이전 실행의 종료
         * 시점에 달려 있어 알 수 없으므로, 지금부터 한 주기 뒤를 상한으로 안내한다.
         */
        LocalDateTime nextSync = LocalDateTime.now(clock).plus(Duration.ofMillis(batchFixedDelayMs));

        return ConnectedAccountListDto.builder()
                .accounts(accounts)
                .nextAutoSyncAt(text(nextSync))
                .build();
    }

    @Transactional
    public DisconnectResultDto disconnect(long userId, long accountId) {
        if (mapper.deactivate(accountId, userId) == 0) {
            throw new BusinessException("NOT_FOUND", "연결된 계좌를 찾을 수 없어요.");
        }
        return DisconnectResultDto.builder().accountId(accountId).disconnected(true).build();
    }

    /**
     * 계좌 하나를 다시 조회한다.
     *
     * ⚠ 트랜잭션을 걸지 않는다. 외부 API 를 부르는 동안 커넥션을 붙잡고 있으면 풀이 마른다
     * (이 클래스의 원칙). 갱신은 계좌 단위로 독립적이라 한 덩어리로 묶을 이유도 없다.
     */
    public ResyncResultDto resync(long userId, long accountId) {
        ConnectedAccount account = mapper.findByIdAndUser(accountId, userId);
        if (account == null) {
            throw new BusinessException("NOT_FOUND", "연결된 계좌를 찾을 수 없어요.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        SyncStatus status = syncInstitution(userId, account.getBankCode(), List.of(account), now);
        return ResyncResultDto.builder()
                .accountId(accountId)
                .syncStatus(status.name())
                .lastSyncAt(text(now))
                .build();
    }

    /**
     * 연결된 모든 계좌를 다시 조회한다 (AC_02_03).
     *
     * ⚠ **공급자를 실제로 부른다.** 예전에는 DB 에 `NORMAL` 과 현재시각만 써서,
     *   조회한 적이 없는데도 "방금 동기화됨" 이 뜨고 잔액은 그대로였다.
     * ⚠ 트랜잭션을 걸지 않는다 — 기관 수만큼 외부 호출이 이어지므로 커넥션을 붙잡으면 안 된다.
     */
    public RefreshResultDto refresh(long userId) {
        List<ConnectedAccount> accounts = mapper.findActiveByUser(userId);
        if (accounts.isEmpty()) {
            throw new BusinessException("NOT_FOUND", "연결된 계좌가 없어요.");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        /* 기관 단위로 묶는다. 조회는 기관 한 번에 계좌 여럿을 돌려주므로 계좌마다 부를 이유가 없다. */
        Map<String, List<ConnectedAccount>> byBank = new LinkedHashMap<>();
        for (ConnectedAccount account : accounts) {
            byBank.computeIfAbsent(account.getBankCode(), code -> new ArrayList<>()).add(account);
        }

        List<RefreshInstitutionDto> rows = new ArrayList<>();
        for (Map.Entry<String, List<ConnectedAccount>> entry : byBank.entrySet()) {
            List<ConnectedAccount> owned = entry.getValue();
            SyncStatus status = syncInstitution(userId, entry.getKey(), owned, now);

            /*
             * 기관 행의 금액은 **그 기관 계좌의 합계**다.
             * 예전에는 computeIfAbsent 로 첫 계좌만 담아, 같은 은행의 두 번째 계좌부터 버려졌다.
             */
            BigDecimal total = BigDecimal.ZERO;
            for (ConnectedAccount account : owned) {
                total = total.add(account.getBalance() == null ? BigDecimal.ZERO : account.getBalance());
            }

            rows.add(RefreshInstitutionDto.builder()
                    .bankCode(entry.getKey())
                    .bankName(owned.get(0).getBankName())
                    .shortLabel(catalog.shortLabelOf(entry.getKey()))
                    /*
                     * 새 거래 건수는 거래내역 수집(후속 이슈)이 붙어야 실제 값이 나온다.
                     * 지금은 0 으로 두고 화면이 "새 거래 없음" 을 그리게 한다 — 지어내지 않는다.
                     */
                    .newTransactionCount(0)
                    .balance(total)
                    .syncStatus(status.name())
                    .build());
        }

        return RefreshResultDto.builder()
                .lastSyncAt(text(now))
                .institutionCount(rows.size())
                .cooldownSeconds(REFRESH_COOLDOWN_SECONDS)
                .institutions(List.copyOf(rows))
                .build();
    }

    /**
     * 한 기관을 조회해 그 기관 계좌들의 잔액을 갱신한다. 갱신된 잔액은 인자로 받은 객체에도 반영한다.
     *
     * 연결이 살아 있지 않으면 **거짓으로 성공을 쓰지 않는다** — 계좌를 `NEED_RECONNECT` 로 표시해
     * 사용자가 재연동할 수 있게 한다. 그 밖의 실패는 `FAILED` 와 사유를 남긴다.
     *
     * @return 이 기관의 조회 결과 상태
     */
    private SyncStatus syncInstitution(long userId, String bankCode,
                                       List<ConnectedAccount> owned, LocalDateTime now) {
        String connectionId = owned.get(0).getCodefConnectedId();
        Map<String, BigDecimal> fresh = new LinkedHashMap<>();
        try {
            for (FinancialAccountDto found : client.fetchAccounts(connectionId, bankCode)) {
                fresh.put(accountNumbers.hash(bankCode, found.getAccountNo()),
                        found.getBalance() == null ? BigDecimal.ZERO : found.getBalance());
            }
        } catch (BusinessException e) {
            /*
             * 연결이 끊겼거나 만료된 경우와 일시적 실패를 구분한다.
             * 전자는 사용자가 재연동해야 풀리고, 후자는 다시 눌러보면 된다.
             */
            SyncStatus status = "CONNECTION_NOT_FOUND".equals(e.getCode())
                    || "TOKEN_EXPIRED".equals(e.getCode())
                    ? SyncStatus.NEED_RECONNECT
                    : SyncStatus.FAILED;
            for (ConnectedAccount account : owned) {
                mapper.updateSync(account.getId(), userId, status.name(), now, e.getMessage());
                if (status == SyncStatus.NEED_RECONNECT) {
                    events.publishEvent(reconnectNotification(userId, account));
                }
            }
            return status;
        }

        /*
         * ⚠ 계좌 하나라도 재연동 대상이 되면 기관 결과도 NEED_RECONNECT 다 (이슈 #70).
         *   예전에는 이 아래에서 무조건 NORMAL 을 돌려줘, DB·알림은 "재연동 필요" 인데
         *   화면만 "정상" 으로 보이는 모순이 있었다.
         */
        SyncStatus aggregate = SyncStatus.NORMAL;
        for (ConnectedAccount account : owned) {
            BigDecimal balance = fresh.get(account.getAccountNoEncrypted());
            if (balance == null) {
                /* 기관에서 사라진 계좌(해지 등). 잔액을 지어내지 않고 재연동 대상으로 둔다. */
                mapper.updateSync(account.getId(), userId, SyncStatus.NEED_RECONNECT.name(), now,
                        "금융기관에서 이 계좌를 찾지 못했어요.");
                events.publishEvent(reconnectNotification(userId, account));
                aggregate = SyncStatus.NEED_RECONNECT;
                continue;
            }
            /* 살아 있는 계좌는 그대로 갱신한다 — 하나가 실패했다고 나머지를 버리지 않는다 */
            mapper.updateSynced(account.getId(), userId, balance, now);
            account.setBalance(balance);
        }
        return aggregate;
    }

    /**
     * 재연동이 필요하다는 알림 요청.
     *
     * 문구 자체는 {@link NotificationType} 이 소유한다 — 여기서는 은행명만 넘긴다 (이슈 #68).
     * 딥링크 경로는 계좌 화면을 아는 이 모듈이 만든다.
     */
    private static NotificationRequestedEvent reconnectNotification(long userId,
                                                                    ConnectedAccount account) {
        return new NotificationRequestedEvent(
                userId,
                NotificationType.ACCOUNT_RECONNECT,
                Map.of("bankName", account.getBankName()),
                "/asset/accounts/" + account.getId() + "/reconnect");
    }

    /** 금융정보 동의 철회 시 모든 연동을 해제한다 (이슈 #13 이 남긴 TODO(#12)). */
    @Transactional
    public int disconnectAll(long userId) {
        return mapper.deactivateAllByUser(userId);
    }

    /* ── 내부 ──────────────────────────────────────────── */

    /**
     * 기관코드가 비어 있으면 여기서 막는다.
     *
     * ⚠ 예전에는 리스트가 비었는지만 봤다. null 원소가 통과하면 `catalog.nameOf(null)` 이 조용히
     * null 을 돌려주고, 진행 상태에 `null` 키가 들어가 `nextWaiting()` 이 그 null 을 반환한다.
     * 호출부는 null 을 "할 일 없음" 으로 읽으므로 **폴링이 영원히 끝나지 않는다.**
     */
    private static void requireOrganizations(List<String> organizations) {
        if (organizations.isEmpty() || organizations.stream().anyMatch(
                code -> code == null || code.isBlank())) {
            throw new BusinessException("EXTERNAL_API_ERROR", "연결할 금융기관을 선택해 주세요.");
        }
    }

    private void rememberProgress(long userId, ConnectionResult result, List<String> organizations) {
        List<String> success = result.getSuccessOrganizations() == null
                ? organizations
                : result.getSuccessOrganizations();
        List<String> failed = result.getFailedOrganizations() == null
                ? List.of()
                : result.getFailedOrganizations();

        /*
         * 성공한 곳과 **실패한 곳을 모두** 목록에 올린다.
         * ⚠ 예전에는 성공한 곳만 올렸고, `mark()` 는 목록에 없는 기관을 조용히 무시한다.
         *   그래서 인증에 실패한 기관이 진행 화면에서 통째로 사라졌고,
         *   전부 실패하면 목록이 비어 `done=true` 인데 `percent=0` 인 모순 응답이 나갔다.
         * 요청한 기관 전체를 올리지는 않는다 — 간편인증은 첫 기관만 처리하므로
         *   시도하지도 않은 기관이 영영 대기 상태로 남는다.
         */
        Map<String, String> targets = new LinkedHashMap<>();
        for (String code : success) {
            targets.put(code, catalog.nameOf(code));
        }
        for (String code : failed) {
            targets.put(code, catalog.nameOf(code));
        }

        LinkProgress progress = new LinkProgress(
                userId, result.getConnectionId(), targets, clock.instant());

        /* 인증 단계에서 이미 실패한 기관은 조회를 시도하지 않고 바로 실패로 표시한다. */
        for (String code : failed) {
            progress.mark(code, ProgressStatus.FAILED);
        }
        progressStore.put(progress);
    }

    private LinkProgressDto toProgressDto(LinkProgress progress) {
        List<ProgressRowDto> rows = progress.getStatuses().entrySet().stream()
                .map(entry -> ProgressRowDto.builder()
                        .code(entry.getKey())
                        .name(progress.nameOf(entry.getKey()))
                        .status(entry.getValue().name())
                        .build())
                .toList();
        return LinkProgressDto.builder()
                .percent(progress.percent())
                .done(progress.isDone())
                .institutions(rows)
                .build();
    }

    private static ConnectionResponseDto toResponse(ConnectionResult result) {
        return ConnectionResponseDto.builder()
                .connectionId(result.getConnectionId())
                .status(result.getStatus().name())
                .expiresInSeconds(result.getExpiresInSeconds())
                .needsExtraAuth(result.isNeedsExtraAuth())
                .extraAuthType(result.getExtraAuthType())
                .build();
    }

    private String bankName(FinancialAccountDto account) {
        return account.getBankName() != null
                ? account.getBankName()
                : catalog.nameOf(account.getOrganization());
    }

    /**
     * 계좌 종류 판정.
     *
     * 공급자가 이미 판정해 줬으면(목서버) 그 값을 쓰고, 코드만 왔으면(실 CODEF) 유도한다.
     * CODEF 예금 구분 `11` = 보통예금(입출금), 그 외 적립·거치식은 예적금으로 본다.
     */
    private static String accountType(FinancialAccountDto account) {
        String given = account.getAccountType();
        if ("DEMAND_DEPOSIT".equals(given) || "SAVINGS".equals(given)) {
            return given;
        }
        return "11".equals(account.getDepositTypeCode()) || account.getDepositTypeCode() == null
                ? "DEMAND_DEPOSIT"
                : "SAVINGS";
    }

    private static String providerName(String code) {
        return switch (code) {
            case "KAKAO" -> "카카오톡";
            case "PASS" -> "PASS";
            case "NAVER" -> "네이버";
            default -> code;
        };
    }

    private static String text(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}

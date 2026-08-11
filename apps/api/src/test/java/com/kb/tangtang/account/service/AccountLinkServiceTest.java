package com.kb.tangtang.account.service;

import com.kb.tangtang.account.client.FinancialDataClient;
import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.account.domain.AuthMethod;
import com.kb.tangtang.account.domain.AuthStatus;
import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.account.dto.*;
import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 연동 흐름 검증.
 *
 * FinancialDataClient 를 가짜로 바꿔 목/실 어느 쪽에도 의존하지 않고 흐름만 본다 —
 * 이 경계가 제대로 서 있는지 자체가 검증 대상이다.
 */
class AccountLinkServiceTest {

    private static final long USER_ID = 1L;
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-05T09:00:00Z"), ZoneId.of("Asia/Seoul"));

    private FinancialDataClient client;
    private ConnectedAccountMapper mapper;
    private LinkProgressStore store;
    private ConsentService consentService;
    private AccountLinkService service;

    /* 발행된 이벤트를 모아두는 퍼블리셔. 계좌별로 정확히 한 번 발행되는지 검증한다 */
    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher capturingPublisher = publishedEvents::add;

    @BeforeEach
    void setUp() {
        client = mock(FinancialDataClient.class);
        mapper = mock(ConnectedAccountMapper.class);
        store = new LinkProgressStore(CLOCK);
        consentService = mock(ConsentService.class);
        service = new AccountLinkService(client, mapper, store, new InstitutionCatalog(),
                new AccountNumberPolicy("test-secret-key-for-account-hash-0001"), capturingPublisher,
                consentService, CLOCK);
        /*
         * 기본은 "제3자 제공 동의를 마친 사용자" 다. 동의 검사는 연결 생성의 전제일 뿐이라
         * 여기 대부분의 테스트가 검증하려는 대상이 아니다. 미동의 경로는 별도 테스트에서 덮어쓴다.
         */
        when(consentService.needsConsent(anyLong(), any())).thenReturn(false);

        when(mapper.findActiveByUser(anyLong())).thenReturn(List.of());
        when(mapper.findActiveHashes(anyLong())).thenReturn(List.of());
        /* 기본은 "제한 없음" — 목서버가 그렇다. 제한하는 경우는 개별 테스트에서 덮어쓴다. */
        when(client.supportedOrganizations()).thenReturn(Set.of());
    }

    private ConnectionResult approvedConnection(List<String> organizations) {
        return ConnectionResult.builder()
                .connectionId("conn-1")
                .status(AuthStatus.PENDING)
                .expiresInSeconds(300)
                .needsExtraAuth(false)
                .successOrganizations(organizations)
                .failedOrganizations(List.of())
                .build();
    }

    private FinancialAccountDto account(String organization, String name, String no) {
        return FinancialAccountDto.builder()
                .organization(organization)
                .accountName(name)
                .accountNo(no)
                .depositTypeCode("11")
                .currency("KRW")
                .balance(new BigDecimal("8340000"))
                .build();
    }

    @Test
    @DisplayName("인증 수단은 공급자가 정한다 - 서비스가 목/실을 판단하지 않는다")
    void authMethodsComeFromClient() {
        when(client.supportedAuthMethods()).thenReturn(List.of(AuthMethod.SIMPLE_AUTH));
        when(client.simpleAuthProviders()).thenReturn(List.of("KAKAO"));

        AuthMethodListDto result = service.authMethods();

        assertEquals(1, result.getMethods().size());
        assertEquals("SIMPLE_AUTH", result.getMethods().get(0).getType());
        assertEquals("카카오톡", result.getMethods().get(0).getProviders().get(0).getName());
    }

    /*
     * 제3자 제공 동의 검사 (DECISIONS.md 2026-08-11 (7)).
     * 화면 순서에만 맡기면 우회된다 — 계좌 연동 진입점이 셋이고 전부 동의 화면을 거치지 않았다.
     */
    @Test
    @DisplayName("제3자 제공 동의가 없으면 간편인증을 시작하지 않는다 — 공급자를 부르기도 전에 막는다")
    void simpleAuthRequiresFinancialConsent() {
        when(consentService.needsConsent(anyLong(), any())).thenReturn(true);
        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requestSimpleAuth(USER_ID, request));

        assertEquals("CONSENT_REQUIRED", e.getCode());
        verify(client, never()).createConnection(any());
    }

    @Test
    @DisplayName("제3자 제공 동의가 없으면 기관 로그인도 막는다 — 진입점이 달라도 규칙은 같다")
    void institutionLoginRequiresFinancialConsent() {
        when(consentService.needsConsent(anyLong(), any())).thenReturn(true);
        CredentialRequestDto row = new CredentialRequestDto();
        row.setOrganization("0004");
        row.setId("tester");
        row.setPassword("pw");
        InstitutionLoginRequestDto request = new InstitutionLoginRequestDto();
        request.setCredentials(List.of(row));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requestInstitutionLogin(USER_ID, request));

        assertEquals("CONSENT_REQUIRED", e.getCode());
        verify(client, never()).createConnection(any());
    }

    @Test
    @DisplayName("기관을 고르지 않으면 간편인증을 시작하지 않는다")
    void simpleAuthNeedsOrganizations() {
        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requestSimpleAuth(USER_ID, request));
        assertEquals("EXTERNAL_API_ERROR", e.getCode());
    }

    @Test
    @DisplayName("승인 전에는 조회를 시작하지 않는다")
    void doesNotFetchBeforeApproval() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.PENDING);

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);

        LinkProgressDto progress = service.progress(USER_ID, "conn-1");

        assertEquals(0, progress.getPercent());
        assertFalse(progress.isDone());
        verify(client, never()).fetchAccounts(any(), any());
    }

    @Test
    @DisplayName("폴링할 때마다 기관을 하나씩 조회하고 끝나면 done 이 된다")
    void fetchesOneInstitutionPerPoll() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004", "0088")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenReturn(List.of(account("0004", "입출금통장", "110123456723")));
        when(client.fetchAccounts("conn-1", "0088"))
                .thenReturn(List.of(account("0088", "신한 주거래", "110987654321")));

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004", "0088"));
        service.requestSimpleAuth(USER_ID, request);

        LinkProgressDto first = service.progress(USER_ID, "conn-1");
        assertEquals(50, first.getPercent());
        assertFalse(first.isDone());

        LinkProgressDto second = service.progress(USER_ID, "conn-1");
        assertEquals(100, second.getPercent());
        assertTrue(second.isDone());
    }

    @Test
    @DisplayName("한 기관이 실패해도 나머지는 계속 진행한다")
    void oneFailureDoesNotStopTheRest() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004", "0088")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "점검 중"));
        when(client.fetchAccounts("conn-1", "0088"))
                .thenReturn(List.of(account("0088", "신한 주거래", "110987654321")));

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004", "0088"));
        service.requestSimpleAuth(USER_ID, request);

        service.progress(USER_ID, "conn-1");
        LinkProgressDto done = service.progress(USER_ID, "conn-1");

        assertTrue(done.isDone());
        assertEquals("FAILED", done.getInstitutions().get(0).getStatus());
        assertEquals("DONE", done.getInstitutions().get(1).getStatus());
    }

    @Test
    @DisplayName("계좌 목록은 마스킹된 번호만 내려보낸다")
    void linkableAccountsAreMasked() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenReturn(List.of(account("0004", "입출금통장", "110123456723")));

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        List<LinkableGroupDto> groups = service.linkableAccounts(USER_ID, "conn-1");

        assertEquals(1, groups.size());
        LinkableAccountDto row = groups.get(0).getAccounts().get(0);
        assertEquals("110-***-****23", row.getAccountNoMasked());
        assertFalse(row.isAlreadyLinked());
        assertEquals("KB국민은행", groups.get(0).getBankName());
    }

    @Test
    @DisplayName("선택한 계좌를 저장한다 - 원본 계좌번호는 남기지 않는다")
    void savesSelectedAccounts() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenReturn(List.of(account("0004", "입출금통장", "110123456723")));
        when(mapper.reactivate(any())).thenReturn(0);

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        LinkRequestDto linkRequest = new LinkRequestDto();
        linkRequest.setConnectionId("conn-1");
        linkRequest.setAccountIds(List.of(1L));

        LinkResultDto result = service.link(USER_ID, linkRequest);

        assertEquals(1, result.getLinkedCount());

        List<ConnectedAccount> saved = new ArrayList<>();
        verify(mapper).insert(argThat(row -> {
            saved.add(row);
            return true;
        }));
        ConnectedAccount row = saved.get(0);
        assertEquals("입출금통장", row.getAccountName());
        assertEquals("110-***-****23", row.getAccountNoMasked());
        assertNotEquals("110123456723", row.getAccountNoEncrypted());
        assertEquals(64, row.getAccountNoEncrypted().length()); // HMAC-SHA256 hex
    }

    @Test
    @DisplayName("공급자가 판정해 준 계좌 종류를 그대로 쓰고 코드 컬럼을 오염시키지 않는다")
    void doesNotPutLongValueIntoDepositTypeCode() {
        /*
         * 목서버는 accountType 을 'DEMAND_DEPOSIT' 로 내려준다.
         * 이 값을 deposit_type_code(VARCHAR(5)) 에 넣으면 저장이 깨진다 — 2026-08-05 실제 발생.
         */
        FinancialAccountDto fromMock = FinancialAccountDto.builder()
                .organization("0004")
                .accountName("KB Star 입출금통장")
                .accountNo("123456******7890")
                .accountType("DEMAND_DEPOSIT")
                .depositTypeCode(null)
                .currency("KRW")
                .balance(new BigDecimal("1244200"))
                .build();

        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004")).thenReturn(List.of(fromMock));
        when(mapper.reactivate(any())).thenReturn(0);

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        LinkRequestDto linkRequest = new LinkRequestDto();
        linkRequest.setConnectionId("conn-1");
        linkRequest.setAccountIds(List.of(1L));
        service.link(USER_ID, linkRequest);

        List<ConnectedAccount> saved = new ArrayList<>();
        verify(mapper).insert(argThat(row -> {
            saved.add(row);
            return true;
        }));
        ConnectedAccount row = saved.get(0);
        assertEquals("DEMAND_DEPOSIT", row.getAccountType());
        assertTrue(row.getDepositTypeCode() == null || row.getDepositTypeCode().length() <= 5,
                "deposit_type_code 는 VARCHAR(5) 다. 코드값만 담아야 한다: " + row.getDepositTypeCode());
    }

    @Test
    @DisplayName("코드만 온 경우(실 CODEF)에는 종류를 유도한다")
    void derivesAccountTypeFromCode() {
        FinancialAccountDto savings = FinancialAccountDto.builder()
                .organization("0004")
                .accountName("KB 정기적금")
                .accountNo("210123456707")
                .accountType(null)
                .depositTypeCode("12")
                .currency("KRW")
                .balance(new BigDecimal("1200000"))
                .build();

        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004")).thenReturn(List.of(savings));
        when(mapper.reactivate(any())).thenReturn(0);

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        LinkRequestDto linkRequest = new LinkRequestDto();
        linkRequest.setConnectionId("conn-1");
        linkRequest.setAccountIds(List.of(1L));
        service.link(USER_ID, linkRequest);

        List<ConnectedAccount> saved = new ArrayList<>();
        verify(mapper).insert(argThat(row -> {
            saved.add(row);
            return true;
        }));
        assertEquals("SAVINGS", saved.get(0).getAccountType());
        assertEquals("12", saved.get(0).getDepositTypeCode());
    }

    @Test
    @DisplayName("이미 연결된 계좌만 골랐다면 연결하지 않는다")
    void skipsAlreadyLinked() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        when(mapper.findActiveHashes(anyLong()))
                .thenReturn(List.of(policy.hash("0004", "110123456723")));
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenReturn(List.of(account("0004", "입출금통장", "110123456723")));

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        LinkRequestDto linkRequest = new LinkRequestDto();
        linkRequest.setConnectionId("conn-1");
        linkRequest.setAccountIds(List.of(1L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.link(USER_ID, linkRequest));
        assertEquals("EXTERNAL_API_ERROR", e.getCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("공급자가 다루지 못하는 기관은 목록에서 뺀다")
    void hidesUnsupportedInstitutions() {
        /* 실 CODEF 는 카카오뱅크(0090)·토스뱅크(0092)를 제공하지 않는다. */
        when(client.supportedOrganizations()).thenReturn(Set.of("0004", "0003"));

        InstitutionListDto result = service.institutions(USER_ID);

        List<String> codes = result.getBanks().stream().map(InstitutionDto::getCode).toList();
        assertEquals(List.of("0004", "0003"), codes);
        /* 카드·증권·보험도 같은 기준으로 걸린다 — 지금 구현은 은행 계좌조회뿐이다. */
        assertTrue(result.getCards().isEmpty());
        assertTrue(result.getSecurities().isEmpty());
    }

    @Test
    @DisplayName("제한이 없으면 목록을 그대로 내려준다 (목서버)")
    void keepsAllInstitutionsWhenUnrestricted() {
        InstitutionListDto result = service.institutions(USER_ID);

        List<String> codes = result.getBanks().stream().map(InstitutionDto::getCode).toList();
        assertTrue(codes.contains("0090"), "목 모드에는 카카오뱅크가 남아야 한다");
        assertFalse(result.getCards().isEmpty());
    }

    @Test
    @DisplayName("같은 계좌를 두 번 보내도 한 번만 센다")
    void countsDuplicateIdsOnce() {
        when(client.createConnection(any())).thenReturn(approvedConnection(List.of("0004")));
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);
        when(client.fetchAccounts("conn-1", "0004"))
                .thenReturn(List.of(account("0004", "입출금통장", "110123456723")));

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);
        service.progress(USER_ID, "conn-1");

        LinkRequestDto linkRequest = new LinkRequestDto();
        linkRequest.setConnectionId("conn-1");
        linkRequest.setAccountIds(List.of(1L, 1L));

        /* 중복을 그대로 두면 reactivate 가 두 번 matched 를 돌려줘 "2개 연결됨" 이 된다. */
        assertEquals(1, service.link(USER_ID, linkRequest).getLinkedCount());
    }

    @Test
    @DisplayName("기관코드가 비어 있으면 요청을 시작하지 않는다")
    void rejectsBlankOrganization() {
        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        /* null 이 통과하면 진행 상태에 null 키가 들어가 폴링이 영원히 끝나지 않는다. */
        request.setOrganizations(java.util.Collections.singletonList(null));

        assertThrows(BusinessException.class, () -> service.requestSimpleAuth(USER_ID, request));
        verify(client, never()).createConnection(any());
    }

    @Test
    @DisplayName("인증에 실패한 기관도 진행 목록에 남는다")
    void keepsFailedInstitutionsVisible() {
        when(client.createConnection(any())).thenReturn(ConnectionResult.builder()
                .connectionId("conn-1")
                .status(AuthStatus.PENDING)
                .expiresInSeconds(300)
                .needsExtraAuth(false)
                .successOrganizations(List.of())
                .failedOrganizations(List.of("0004"))
                .build());
        when(client.getAuthStatus("conn-1")).thenReturn(AuthStatus.APPROVED);

        SimpleAuthRequestDto request = new SimpleAuthRequestDto();
        request.setProvider("KAKAO");
        request.setOrganizations(List.of("0004"));
        service.requestSimpleAuth(USER_ID, request);

        LinkProgressDto progress = service.progress(USER_ID, "conn-1");

        /* 예전에는 실패 기관이 목록에서 사라지고 done=true, percent=0 인 모순 응답이 나갔다. */
        assertEquals(1, progress.getInstitutions().size());
        assertEquals("FAILED", progress.getInstitutions().get(0).getStatus());
        assertTrue(progress.isDone());
        assertEquals(100, progress.getPercent());
    }

    private ConnectedAccount linked(long id, String bankCode, String hash, String balance) {
        return ConnectedAccount.builder()
                .id(id)
                .userId(USER_ID)
                .codefConnectedId("conn-1")
                .bankCode(bankCode)
                .bankName("IBK기업은행")
                .accountNoEncrypted(hash)
                .balance(new BigDecimal(balance))
                .build();
    }

    @Test
    @DisplayName("즉시 조회는 공급자를 실제로 부르고 잔액을 갱신한다")
    void refreshCallsProvider() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        String hash = policy.hash("0003", "110123456723");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(linked(7L, "0003", hash, "100")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenReturn(List.of(account("0003", "입출금통장", "110123456723")));

        RefreshResultDto result = service.refresh(USER_ID);

        /* 예전에는 공급자를 부르지 않고 DB 에 NORMAL 만 써서 "방금 동기화됨" 이 거짓이었다. */
        verify(client).fetchAccounts("conn-1", "0003");
        verify(mapper).updateSynced(eq(7L), eq(USER_ID), eq(new BigDecimal("8340000")), any());
        assertEquals("NORMAL", result.getInstitutions().get(0).getSyncStatus());
    }

    @Test
    @DisplayName("기관 행의 금액은 그 기관 계좌의 합계다")
    void sumsBalancePerInstitution() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(
                linked(1L, "0003", policy.hash("0003", "111"), "0"),
                linked(2L, "0003", policy.hash("0003", "222"), "0")));
        when(client.fetchAccounts("conn-1", "0003")).thenReturn(List.of(
                balanced("0003", "111", "500000"),
                balanced("0003", "222", "1500000")));

        RefreshResultDto result = service.refresh(USER_ID);

        /* 예전에는 computeIfAbsent 로 첫 계좌만 담아 두 번째 계좌가 통째로 버려졌다. */
        assertEquals(1, result.getInstitutions().size());
        assertEquals(0, new BigDecimal("2000000").compareTo(result.getInstitutions().get(0).getBalance()));
    }

    @Test
    @DisplayName("연결이 끊겼으면 성공으로 쓰지 않고 재연동 대상으로 표시한다")
    void marksNeedReconnectWhenConnectionGone() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        when(mapper.findActiveByUser(USER_ID))
                .thenReturn(List.of(linked(9L, "0003", policy.hash("0003", "111"), "100")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenThrow(new BusinessException("CONNECTION_NOT_FOUND", "연결 정보를 찾을 수 없어요."));

        RefreshResultDto result = service.refresh(USER_ID);

        assertEquals("NEED_RECONNECT", result.getInstitutions().get(0).getSyncStatus());
        verify(mapper).updateSync(eq(9L), eq(USER_ID), eq("NEED_RECONNECT"), any(), any());
        verify(mapper, never()).updateSynced(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("[예외 분기] 연결이 끊긴 기관의 계좌마다 재연동 이벤트가 하나씩 발행된다")
    void publishesReconnectEventPerAccountWhenConnectionGone() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(
                linked(9L, "0003", policy.hash("0003", "111"), "100"),
                linked(10L, "0003", policy.hash("0003", "222"), "200")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenThrow(new BusinessException("CONNECTION_NOT_FOUND", "연결 정보를 찾을 수 없어요."));

        RefreshResultDto result = service.refresh(USER_ID);

        /*
         * owned 두 계좌가 모두 NEED_RECONNECT 로 확정되므로, 반환값(SyncStatus) 하나가 아니라
         * 계좌마다 이벤트가 나가야 한다 — "sync 실행당 한 번"이 아니라 "계좌마다 한 번".
         */
        assertEquals("NEED_RECONNECT", result.getInstitutions().get(0).getSyncStatus());
        assertEquals(2, publishedEvents.size());
        List<NotificationRequestedEvent> events = publishedEvents.stream()
                .map(NotificationRequestedEvent.class::cast)
                .toList();
        assertEquals(List.of("/asset/accounts/9/reconnect", "/asset/accounts/10/reconnect"),
                events.stream().map(NotificationRequestedEvent::deepLinkUrl).toList());
        assertTrue(events.stream().allMatch(e ->
                e.userId().equals(USER_ID)
                        && e.type() == NotificationType.ACCOUNT_RECONNECT
                        && "IBK기업은행".equals(e.params().get("bankName"))));
    }

    @Test
    @DisplayName("[잔액 누락 분기] 응답에 없는 계좌는 그 계좌 하나만 재연동 이벤트가 발행된다")
    void publishesReconnectEventWhenAccountMissingFromResponse() {
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        ConnectedAccount account = linked(11L, "0003", policy.hash("0003", "111"), "100");
        when(mapper.findByIdAndUser(11L, USER_ID)).thenReturn(account);
        /* 기관 응답에 이 계좌가 없다 — 해지 등으로 사라진 경우 */
        when(client.fetchAccounts("conn-1", "0003")).thenReturn(List.of());

        ResyncResultDto result = service.resync(USER_ID, 11L);

        /*
         * ⚠ 예전에는 이 분기에서도 응답이 NORMAL 이었다 — DB 와 알림은 "재연동 필요" 인데
         * 화면만 "정상" 으로 보이는 모순이었다 (이슈 #70).
         */
        assertEquals("NEED_RECONNECT", result.getSyncStatus());

        verify(mapper).updateSync(eq(11L), eq(USER_ID), eq("NEED_RECONNECT"), any(),
                eq("금융기관에서 이 계좌를 찾지 못했어요."));
        verify(mapper, never()).updateSynced(eq(11L), anyLong(), any(), any());
        assertEquals(1, publishedEvents.size());
        NotificationRequestedEvent event = (NotificationRequestedEvent) publishedEvents.get(0);
        assertEquals(USER_ID, event.userId().longValue());
        assertEquals(NotificationType.ACCOUNT_RECONNECT, event.type());
        assertEquals("IBK기업은행", event.params().get("bankName"));
        assertEquals("/asset/accounts/11/reconnect", event.deepLinkUrl());
    }

    @Test
    @DisplayName("[집계] 한 계좌라도 재연동이 필요하면 기관 결과도 NEED_RECONNECT 다")
    void institutionStatusReflectsWorstAccount() {
        /*
         * 같은 기관에 계좌가 둘인데 하나만 응답에서 사라진 경우다.
         * 예전에는 성공 경로 끝에서 무조건 NORMAL 을 돌려줘, 한 계좌가 재연동 대상이 돼도
         * 즉시 조회 화면은 "정상" 으로 보였다 (이슈 #70).
         */
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        String presentHash = policy.hash("0003", "110123456723");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(
                linked(7L, "0003", presentHash, "100"),
                linked(8L, "0003", policy.hash("0003", "999"), "200")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenReturn(List.of(account("0003", "입출금통장", "110123456723")));

        RefreshResultDto result = service.refresh(USER_ID);

        assertEquals("NEED_RECONNECT", result.getInstitutions().get(0).getSyncStatus());
        /* 살아 있는 계좌는 그대로 갱신된다 — 하나가 실패했다고 나머지를 버리지 않는다 */
        verify(mapper).updateSynced(eq(7L), eq(USER_ID), any(), any());
        verify(mapper).updateSync(eq(8L), eq(USER_ID), eq("NEED_RECONNECT"), any(), any());
    }

    @Test
    @DisplayName("[정상 동기화] NORMAL 로 끝나면 재연동 이벤트를 발행하지 않는다")
    void doesNotPublishReconnectEventWhenSyncNormal() {
        /* 거짓 알림 방지 가드 — 가장 중요한 케이스다. refreshCallsProvider 와 같은 픽스처를 쓴다. */
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        String hash = policy.hash("0003", "110123456723");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(linked(7L, "0003", hash, "100")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenReturn(List.of(account("0003", "입출금통장", "110123456723")));

        RefreshResultDto result = service.refresh(USER_ID);

        assertEquals("NORMAL", result.getInstitutions().get(0).getSyncStatus());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("[예외 분기 - FAILED] 일시적 실패는 재연동 이벤트를 발행하지 않는다")
    void doesNotPublishReconnectEventOnTransientFailure() {
        /*
         * CONNECTION_NOT_FOUND·TOKEN_EXPIRED 가 아닌 코드는 FAILED 로 갈린다 (oneFailureDoesNotStopTheRest 와 같은 픽스처).
         * FAILED 는 "다시 눌러보면 되는" 일시적 실패라 재연동 알림 대상이 아니다.
         */
        AccountNumberPolicy policy = new AccountNumberPolicy("test-secret-key-for-account-hash-0001");
        when(mapper.findActiveByUser(USER_ID)).thenReturn(List.of(
                linked(12L, "0003", policy.hash("0003", "111"), "100")));
        when(client.fetchAccounts("conn-1", "0003"))
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "점검 중"));

        RefreshResultDto result = service.refresh(USER_ID);

        assertEquals("FAILED", result.getInstitutions().get(0).getSyncStatus());
        assertTrue(publishedEvents.isEmpty());
    }

    private FinancialAccountDto balanced(String organization, String no, String balance) {
        return FinancialAccountDto.builder()
                .organization(organization)
                .accountName("통장")
                .accountNo(no)
                .depositTypeCode("11")
                .currency("KRW")
                .balance(new BigDecimal(balance))
                .build();
    }
}

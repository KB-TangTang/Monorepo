package com.kb.tangtang.account.client;

import com.kb.tangtang.account.client.dto.ConnectionRequest;
import com.kb.tangtang.account.client.dto.ConnectionResult;
import com.kb.tangtang.account.client.dto.CredentialDto;
import com.kb.tangtang.account.client.dto.FinancialAccountDto;
import com.kb.tangtang.account.client.dto.IdentityDto;
import com.kb.tangtang.account.client.dto.TwoWayInfo;
import com.kb.tangtang.account.domain.AuthMethod;
import com.kb.tangtang.account.domain.AuthStatus;
import com.kb.tangtang.common.exception.BusinessException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실 CODEF 구현. 발표의 라이브 데모 1컷이 이 구현으로 돈다.
 *
 * 인증 경로는 **기관 로그인(아이디·비밀번호)** 하나다. CODEF 간편인증은 2-way 추가 통신
 * (인증 요청 → 사용자 승인 대기 → 재요청)이 필요해 데모 리스크가 크다고 판단했다
 * (DECISIONS.md 2026-08-05 결정 3번).
 *
 * 확인된 응답 구조 (2026-08-05 실계좌 테스트):
 *   · POST /v1/account/create      → data.connectedId + successList / errorList (기관 단위)
 *   · POST .../account/account-list → data.resDepositTrust[] 등 5분류
 *   · resAccountDisplay 는 **마스킹되지 않은 전체 계좌번호**다
 *   · CODEF 는 응답 본문을 URL 인코딩해 내려준다 (SDK 가 디코딩해 준다)
 *
 * ⚠ 자격증명은 이 클래스 밖으로 나가지 않는다. 로그로 찍지 않고 필드로 보관하지 않는다.
 *   비밀번호는 CodefPasswordCipher 로 RSA 암호화한 뒤에만 전송한다.
 */
@Log4j2
public class CodefFinancialDataClient implements FinancialDataClient {

    private static final String TOKEN_URL = "https://oauth.codef.io/oauth/token";

    /** 추가 인증이 필요하다는 CODEF 응답 코드. 이게 오면 사용자의 앱 승인을 기다린다. */
    private static final String CONTINUE_TWO_WAY = "CF-03002";

    /** 간편인증 승인 제한 시간. 화면의 남은 시간 표시와 맞춘 값이다. */
    private static final int SIMPLE_AUTH_EXPIRES_SECONDS = 300;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final CodefPasswordCipher passwordCipher;

    public CodefFinancialDataClient(RestTemplate restTemplate, String baseUrl,
                                    String clientId, String clientSecret,
                                    CodefPasswordCipher passwordCipher) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.passwordCipher = passwordCipher;
    }

    /**
     * 간편인증 수단 → CODEF `loginTypeLevel`.
     * 1 카카오톡 · 5 통신사(PASS) · 6 네이버 (CODEF 개발가이드)
     */
    private static final Map<String, String> LOGIN_TYPE_LEVELS =
            Map.of("KAKAO", "1", "PASS", "5", "NAVER", "6");

    /**
     * 진행 중인 연결. **간편인증은 1차 요청 시점에 connectedId 가 없다** —
     * 사용자가 앱에서 승인한 뒤 2차 요청에서야 발급된다. 그래서 우리 식별자를 먼저 만들고
     * 그 아래에 2-way 세션과 나중에 받은 connectedId 를 묶어둔다.
     *
     * 기관 로그인도 같은 통로를 쓴다 — 바깥(서비스·프론트)에서 두 경로가 구분되지 않아야 한다.
     */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** 세션 보관 기한. 인증 제한 시간(5분)에 조회 시간을 얹은 값이다. */
    private static final long SESSION_TTL_SECONDS = 30 * 60L;

    /**
     * 한 번의 연결 시도. 승인 전에는 connectedId 가 null 이다.
     *
     * ⚠ **본인 정보(이름·생년월일·통신사·휴대폰)는 여기 두지 않는다.** 2차 요청에 필요한 것은
     *   `twoWayInfo` 와 `organization` 뿐이라 보관할 이유가 없고, 힙에 남기면 덤프 한 번에 새어 나간다.
     *   (2026-08-05 리뷰 지적 — 쓰이지도 않는 PII 를 무기한 들고 있었다)
     */
    private static final class Session {
        private String connectedId;
        private TwoWayInfo twoWayInfo;
        private String organization;
        private AuthStatus status = AuthStatus.PENDING;
        private final Instant createdAt = Instant.now();
    }

    /**
     * 실 CODEF 로 제공할 인증 수단.
     *
     * **은행권은 간편인증을 쓸 수 없다.** 2026-08-05 실호출로 확인했다 —
     * IBK(0003)·카카오뱅크(0090) 모두 `CF-11021`("기관코드와 로그인 방식을 확인하세요")로 거부됐다.
     * CODEF 계정등록은 기관마다 허용하는 로그인 방식이 정해져 있고, 이 두 곳은 간편인증이 아니다.
     *
     * 그래서 **되는 것만 알린다.** 화면에 띄워놓고 누르면 실패하는 선택지를 두지 않는다.
     * 간편인증 2-way 구현(`startSimpleAuth`·`continueBody`·`getAuthStatus`)은 지우지 않았다 —
     * CODEF 가 지원 기관을 넓히면 이 목록에 `SIMPLE_AUTH` 를 되돌리는 것만으로 켜진다.
     * 목 모드는 그대로 간편인증을 제공한다(시연 기준).
     */
    @Override
    public List<AuthMethod> supportedAuthMethods() {
        return List.of(AuthMethod.INSTITUTION_LOGIN);
    }

    @Override
    public List<String> simpleAuthProviders() {
        return List.of("KAKAO", "PASS", "NAVER");
    }

    /**
     * CODEF 가 개인 계좌조회를 제공하는 은행 20곳 (2026-08-05 공식 문서 `은행 - 개요` 기준).
     *
     * **카카오뱅크(0090)·토스뱅크(0092)는 목록에 없다.** 우리 기관 목록에는 있어서
     * 실 CODEF 모드에서 고르면 `CF-11021` 로 반드시 실패한다 — 그래서 여기서 걸러낸다.
     * 인터넷은행 중 제공되는 곳은 케이뱅크(0089) 하나다.
     * 카드·증권·보험은 상품 경로가 달라 지금 구현(은행 계좌조회)으로는 다루지 못한다.
     */
    private static final Set<String> CODEF_BANKS = Set.of(
            "0002", "0003", "0004", "0007", "0011", "0020", "0023", "0027", "0031", "0032",
            "0034", "0035", "0037", "0039", "0045", "0048", "0071", "0081", "0088", "0089");

    @Override
    public Set<String> supportedOrganizations() {
        return CODEF_BANKS;
    }

    @Override
    public boolean requiresIdentity() {
        /* 실 CODEF 간편인증은 "누구에게 푸시를 보낼지" 를 알아야 한다. */
        return true;
    }

    @Override
    public ConnectionResult createConnection(ConnectionRequest request) {
        if (request.getAuthMethod() == AuthMethod.SIMPLE_AUTH) {
            return startSimpleAuth(request);
        }
        return loginWithCredentials(request);
    }

    /**
     * 간편인증 1차 요청.
     *
     * 정상 흐름은 **여기서 끝나지 않는다** — CODEF 가 `CF-03002`(추가 인증 필요)를 돌려주고,
     * 사용자가 인증 앱에서 승인한 뒤 2차 요청(`continueSimpleAuth`)에서 connectedId 가 나온다.
     * 그래서 우리 식별자만 먼저 발급하고 PENDING 으로 돌려준다.
     *
     * ⚠ 기관을 여러 곳 고르면 CODEF 는 기관마다 별도 인증을 요구한다.
     *   지금은 **첫 기관 하나만** 진행한다. 여러 기관 동시 간편인증은 실호출로 동작을 확인한 뒤 넓힌다.
     */
    private ConnectionResult startSimpleAuth(ConnectionRequest request) {
        List<String> organizations = request.getOrganizations();
        if (organizations == null || organizations.isEmpty()) {
            throw new BusinessException("EXTERNAL_API_ERROR", "연결할 금융기관을 선택해 주세요.");
        }
        String level = LOGIN_TYPE_LEVELS.get(request.getProvider());
        if (level == null) {
            throw new BusinessException("INVALID_CREDENTIALS", "지원하지 않는 인증 수단이에요.");
        }
        IdentityDto identity = request.getIdentity();
        if (identity == null || identity.getUserName() == null || identity.getPhoneNo() == null) {
            throw new BusinessException("INVALID_CREDENTIALS", "본인 정보를 입력해 주세요.");
        }

        String organization = organizations.get(0);
        Map<String, Object> response = post(baseUrl + "/v1/account/create",
                Map.of("accountList", List.of(simpleAuthRow(organization, level, identity))));

        String connectionId = "codef-" + UUID.randomUUID();
        Session session = new Session();
        session.organization = organization;
        evictExpiredSessions();
        sessions.put(connectionId, session);

        applyResponse(session, response);

        return ConnectionResult.builder()
                .connectionId(connectionId)
                .status(session.status)
                .expiresInSeconds(SIMPLE_AUTH_EXPIRES_SECONDS)
                .needsExtraAuth(false)
                .extraAuthType(null)
                .successOrganizations(List.of(organization))
                .failedOrganizations(List.of())
                .build();
    }

    /** 기관 로그인(아이디·비밀번호). create 응답이 곧 결과라 승인 대기가 없다. */
    private ConnectionResult loginWithCredentials(ConnectionRequest request) {
        List<CredentialDto> credentials = request.getCredentials();
        if (credentials == null || credentials.isEmpty()) {
            throw new BusinessException("INVALID_CREDENTIALS", "금융기관 로그인 정보가 필요해요.");
        }

        List<Map<String, String>> accountList = new ArrayList<>();
        for (CredentialDto credential : credentials) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("countryCode", "KR");
            row.put("businessType", "BK");
            row.put("clientType", "P");
            row.put("organization", credential.getOrganization());
            row.put("loginType", "1");
            row.put("id", credential.getId());
            /*
             * ⚠ CODEF 는 평문 비밀번호를 받지 않는다. 공개키로 암호화한 값만 유효하다.
             * 여기서 암호화하고, 평문은 이 줄 밖으로 나가지 않는다.
             */
            row.put("password", passwordCipher.encrypt(credential.getPassword()));
            accountList.add(row);
        }

        Map<String, Object> response =
                post(baseUrl + "/v1/account/create", Map.of("accountList", accountList));
        Map<String, Object> data = asMap(response.get("data"));

        String connectedId = string(data.get("connectedId"));
        if (connectedId == null || connectedId.isBlank()) {
            throw new BusinessException("INVALID_CREDENTIALS",
                    "아이디 또는 비밀번호가 일치하지 않아요.");
        }

        /* 간편인증과 같은 통로를 쓴다 — 바깥에서 두 경로가 구분되지 않아야 한다. */
        String connectionId = "codef-" + UUID.randomUUID();
        Session session = new Session();
        session.connectedId = connectedId;
        session.status = AuthStatus.APPROVED;
        session.organization = credentials.get(0).getOrganization();
        evictExpiredSessions();
        sessions.put(connectionId, session);

        /* 기관 단위 성공·실패가 배열로 온다. 진행 화면의 기관별 상태가 여기서 나온다. */
        return ConnectionResult.builder()
                .connectionId(connectionId)
                .status(AuthStatus.APPROVED)
                .expiresInSeconds(0)
                .needsExtraAuth(false)
                .extraAuthType(null)
                .successOrganizations(organizations(data.get("successList")))
                .failedOrganizations(organizations(data.get("errorList")))
                .build();
    }

    /**
     * 승인 여부 확인. 프론트가 1초 간격으로 부른다.
     *
     * 간편인증은 여기서 **2차 요청을 시도한다.** 사용자가 아직 앱에서 승인하지 않았으면
     * CODEF 가 다시 `CF-03002` 를 돌려주므로 PENDING 을 유지하고, 승인했으면 connectedId 가 온다.
     * 폴링이 곧 재시도인 셈이라 별도 대기 스레드가 필요 없다.
     */
    @Override
    public AuthStatus getAuthStatus(String connectionId) {
        Session session = session(connectionId);
        if (session.status != AuthStatus.PENDING || session.twoWayInfo == null) {
            return session.status;
        }

        try {
            applyResponse(session, post(baseUrl + "/v1/account/create", continueBody(session)));
        } catch (BusinessException e) {
            /*
             * 폴링 중 CODEF 가 오류를 주면 인증이 끝난 것이다 — 사용자가 거절했거나 세션이 만료됐다.
             * 예외를 그대로 올리면 화면이 "다시 시도" 대신 오류 배너만 띄우고 상태가 멈춘다.
             */
            session.status = AuthStatus.FAILED;
        }
        return session.status;
    }

    /** 2차 요청 본문. 1차에서 받은 twoWayInfo 를 **그대로** 실어야 같은 세션으로 이어진다. */
    private Map<String, Object> continueBody(Session session) {
        TwoWayInfo info = session.twoWayInfo;
        Map<String, Object> twoWay = new LinkedHashMap<>();
        twoWay.put("jobIndex", info.getJobIndex());
        twoWay.put("threadIndex", info.getThreadIndex());
        twoWay.put("jti", info.getJti());
        twoWay.put("twoWayTimestamp", info.getTwoWayTimestamp());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("organization", session.organization);
        row.put("simpleAuth", "1");
        row.put("is2Way", true);
        row.put("twoWayInfo", twoWay);

        return Map.of("accountList", List.of(row));
    }

    /**
     * 1차·2차 응답을 세션에 반영한다.
     *
     * `CF-03002` + `continue2Way` 면 아직 승인 전이다 — 2-way 세션을 갱신하고 PENDING 을 유지한다.
     * connectedId 가 오면 승인이다. 둘 다 아니면 실패로 본다.
     */
    private void applyResponse(Session session, Map<String, Object> response) {
        Map<String, Object> result = asMap(response.get("result"));
        Map<String, Object> data = asMap(response.get("data"));
        String code = string(result.get("code"));

        if (CONTINUE_TWO_WAY.equals(code) || Boolean.TRUE.equals(data.get("continue2Way"))) {
            session.twoWayInfo = TwoWayInfo.builder()
                    .jobIndex(number(data.get("jobIndex")))
                    .threadIndex(number(data.get("threadIndex")))
                    .jti(string(data.get("jti")))
                    .twoWayTimestamp(number(data.get("twoWayTimestamp")))
                    .build();
            session.status = AuthStatus.PENDING;
            return;
        }

        String connectedId = string(data.get("connectedId"));
        if (connectedId != null && !connectedId.isBlank()) {
            session.connectedId = connectedId;
            session.status = AuthStatus.APPROVED;
            return;
        }

        /* 사용자가 거절했거나 세션이 만료된 경우. 화면은 "다시 시도" 를 보여준다. */
        session.status = AuthStatus.FAILED;
    }

    /**
     * 화면에서 인증번호를 입력해 제출하는 경로(SMS 등).
     *
     * 간편인증은 사용자가 **앱에서** 승인하므로 이 통로를 쓰지 않는다 — 폴링이 재시도를 겸한다.
     * 기관이 화면 입력형 추가인증을 요구하는 경우는 실호출로 확인한 뒤 붙인다.
     */
    @Override
    public AuthStatus submitExtraAuth(String connectionId, String authCode) {
        return getAuthStatus(connectionId);
    }

    /**
     * 지난 세션을 버린다.
     *
     * 이 빈은 싱글턴이라 지우지 않으면 맵이 WAS 수명 내내 자란다 —
     * 연결 시도 한 번마다 엔트리 하나가 영구히 남았다(2026-08-05 리뷰 지적).
     * 별도 스케줄러를 두지 않고 새 세션을 만들 때 함께 치운다. 진행 상태 저장소와 같은 방식이다.
     */
    private void evictExpiredSessions() {
        Instant deadline = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        sessions.values().removeIf(session -> session.createdAt.isBefore(deadline));
    }

    private Session session(String connectionId) {
        Session session = connectionId == null ? null : sessions.get(connectionId);
        if (session == null) {
            throw new BusinessException("CONNECTION_NOT_FOUND",
                    "연결 정보를 찾을 수 없어요. 처음부터 다시 시도해 주세요.");
        }
        return session;
    }

    @Override
    public List<FinancialAccountDto> fetchAccounts(String connectionId, String organization) {
        Session session = session(connectionId);
        if (session.connectedId == null) {
            throw new BusinessException("TOKEN_EXPIRED", "인증이 만료됐어요. 다시 연결해 주세요.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        /* 바깥은 우리 식별자를 쓴다. 실제 CODEF connectedId 로 바꾸는 곳은 여기 한 곳이다. */
        body.put("connectedId", session.connectedId);
        body.put("organization", organization);

        Map<String, Object> response = post(baseUrl + "/v1/kr/bank/p/account/account-list", body);
        Map<String, Object> data = asMap(response.get("data"));

        List<FinancialAccountDto> accounts = new ArrayList<>();
        /* 5분류 중 예적금·입출금만 계좌 연동 대상이다. 대출·보험·펀드는 별도 이슈다. */
        for (Object item : asList(data.get("resDepositTrust"))) {
            Map<String, Object> row = asMap(item);
            accounts.add(FinancialAccountDto.builder()
                    .organization(organization)
                    .bankName(null)
                    .accountName(string(row.get("resAccountName")))
                    /* ⚠ 마스킹되지 않은 전체 계좌번호다. 저장·표시 전에 반드시 가공한다. */
                    .accountNo(string(row.get("resAccountDisplay")))
                    /* 실 CODEF 는 종류를 판정해 주지 않는다. 숫자 코드만 오므로 서비스가 유도한다. */
                    .accountType(null)
                    .depositTypeCode(string(row.get("resAccountDeposit")))
                    .currency(string(row.get("resAccountCurrency")))
                    .balance(decimal(row.get("resAccountBalance")))
                    .build());
        }
        return accounts;
    }

    /** 액세스 토큰 발급. 유효기간이 7일이라 매 호출마다 새로 받아도 부담이 없다. */
    private String accessToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "CODEF 연동 설정이 없어요. financial.codef.client-id 를 확인해 주세요.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)));

        try {
            Map<?, ?> token = restTemplate.postForObject(
                    TOKEN_URL + "?grant_type=client_credentials&scope=read",
                    new HttpEntity<>(headers), Map.class);
            String value = token == null ? null : string(token.get("access_token"));
            if (value == null) {
                throw new BusinessException("TOKEN_EXPIRED", "인증이 만료됐어요. 다시 연결해 주세요.");
            }
            return value;
        } catch (RestClientException e) {
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE",
                    "금융기관이 점검 중이에요. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * CODEF 는 응답 본문을 URL 인코딩해 내려준다. SDK 를 쓰지 않으므로 여기서 디코딩한다.
     * (PLAN_목서버_구축.md Phase 2 가 착수 전 확인하라고 못박은 지점)
     */
    private Map<String, Object> post(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken());

        String raw;
        try {
            raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (RestClientException e) {
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE",
                    "금융기관이 점검 중이에요. 잠시 후 다시 시도해 주세요.");
        }
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("EXTERNAL_API_ERROR", "금융기관 연동에 실패했어요.");
        }
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        return parse(decoded);
    }

    /**
     * 응답 파싱. Jackson 을 직접 쓰지 않고 RestTemplate 의 변환기를 재사용하기 위해
     * 디코딩한 문자열을 다시 Map 으로 읽는다.
     */
    private Map<String, Object> parse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = mapper.readValue(json, HashMap.class);
            Map<String, Object> result = asMap(parsed.get("result"));
            String code = string(result.get("code"));
            /*
             * CF-03002 는 오류가 아니라 **정상 흐름**이다 — "사용자의 앱 승인을 기다리는 중".
             * 여기서 예외로 던지면 간편인증이 시작조차 못 한다. 호출부가 상태로 다룬다.
             */
            if (code != null && !"CF-00000".equals(code) && !CONTINUE_TWO_WAY.equals(code)) {
                logFailure(code, result, parsed);
                throw failure(result, parsed);
            }
            return parsed;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException("EXTERNAL_API_ERROR", "금융기관 응답을 읽지 못했어요.");
        }
    }

    /** 기관이 그 로그인 방식을 지원하지 않을 때 CODEF 가 주는 코드. */
    private static final String UNSUPPORTED_LOGIN_TYPE = "CF-11021";

    /**
     * CODEF 실패를 사용자가 **무엇을 하면 되는지 아는** 오류로 바꾼다.
     *
     * 최상위 메시지("사용자 계정정보 등록에 실패했습니다")는 화면에 띄워도 다음 행동이 나오지 않는다.
     * 원인은 `errorList` 에 있고, 그중 `CF-11021`(기관과 로그인 방식 불일치)은 우리가 정확히 안내할 수 있다 —
     * 그 기관은 간편인증을 지원하지 않으니 금융기관 로그인으로 가면 된다.
     * 나머지는 CODEF 메시지를 그대로 전한다(지어내지 않는다).
     */
    static BusinessException failure(Map<String, Object> result, Map<String, Object> parsed) {
        for (Object item : asList(asMap(parsed.get("data")).get("errorList"))) {
            if (UNSUPPORTED_LOGIN_TYPE.equals(string(asMap(item).get("code")))) {
                return new BusinessException("AUTH_METHOD_UNSUPPORTED",
                        "이 금융기관은 선택한 인증 방식을 지원하지 않아요. 금융기관 로그인으로 연결해 주세요.");
            }
        }
        String message = string(result.get("message"));
        return new BusinessException("EXTERNAL_API_ERROR",
                message == null ? "금융기관 연동에 실패했어요." : message);
    }

    /**
     * CODEF 가 실패를 돌려줬을 때 **원인을 찾을 수 있을 만큼만** 남긴다.
     *
     * 최상위 `result.message` 는 "사용자 계정정보 등록에 실패했습니다" 처럼 뭉뚱그려 오고,
     * 진짜 이유(어떤 파라미터가 틀렸는지)는 `data.errorList[].code/message` 에 기관 단위로 들어 있다.
     * 실호출로 파라미터 이름을 확정해야 하는 간편인증에서는 이 배열이 유일한 단서다.
     *
     * ⚠ 응답 본문 전체를 찍지 않는다 — 이름·계좌번호 같은 개인정보가 섞여 있다.
     *   여기서 남기는 것은 코드·메시지·기관코드뿐이다.
     */
    private static void logFailure(String code, Map<String, Object> result, Map<String, Object> parsed) {
        log.warn("CODEF 응답 실패 code={} message={}", code, string(result.get("message")));
        for (Object item : asList(asMap(parsed.get("data")).get("errorList"))) {
            Map<String, Object> error = asMap(item);
            log.warn("  errorList organization={} code={} message={} extra={}",
                    string(error.get("organization")),
                    string(error.get("code")),
                    string(error.get("message")),
                    string(error.get("extraMessage")));
        }
    }

    private static List<String> organizations(Object listValue) {
        List<String> result = new ArrayList<>();
        for (Object item : asList(listValue)) {
            String organization = string(asMap(item).get("organization"));
            if (organization != null) {
                result.add(organization);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static List<?> asList(Object value) {
        return value instanceof List ? (List<?>) value : List.of();
    }

    /**
     * 간편인증 1차 요청의 계정 항목.
     *
     * ⚠ **확실한 것과 확인이 필요한 것이 섞여 있다.**
     *   확실: `loginType=5`(간편인증) · `loginTypeLevel`(1 카카오톡 · 5 PASS · 6 네이버)
     *   미확인: 본인 정보 필드명과 형식(`userName`·`birthDate`·`phoneNo`·`telecom`).
     *           공개 문서에서 확정하지 못해 일반적인 이름으로 넣었다.
     *           **첫 실호출에서 CODEF 가 누락·형식 오류를 코드로 알려주므로 그때 확정한다.**
     *           지어낸 값이 남지 않도록 이 주석을 지우지 말 것.
     */
    private Map<String, String> simpleAuthRow(String organization, String loginTypeLevel,
                                              IdentityDto identity) {
        String birthDate = toEightDigitBirthDate(identity.getBirthDate());

        Map<String, String> row = new LinkedHashMap<>();
        row.put("countryCode", "KR");
        row.put("businessType", "BK");
        row.put("clientType", "P");
        row.put("organization", organization);
        row.put("loginType", "5");
        row.put("loginTypeLevel", loginTypeLevel);
        row.put("userName", identity.getUserName());
        /*
         * 생년월일을 두 이름으로 함께 보낸다. CODEF 문서마다 `identity` 와 `birthDate` 가 섞여 나오고
         * 어느 쪽이 필수인지 공개 문서로 확정하지 못했다. 값은 같으므로 둘 다 실어 한 번의 실호출로
         * 판정한다 — 응답의 errorList 가 어느 쪽을 요구하는지 알려주면 남는 하나를 지운다.
         */
        row.put("identity", birthDate);
        row.put("birthDate", birthDate);
        row.put("phoneNo", digitsOnly(identity.getPhoneNo()));
        row.put("telecom", telecomCode(identity.getCarrier()));
        return row;
    }

    /**
     * 통신사 → CODEF `telecom` 코드.
     *
     * CODEF 는 통신사를 이름이 아니라 숫자로 받는다. 화면이 보내는 값(`SKT`·`KT_MVNO` …)을
     * 여기서만 바꾼다 — 프론트는 CODEF 를 모른 채로 남아야 한다.
     * 0 SKT · 1 KT · 2 LG U+ · 3 SKT 알뜰폰 · 4 KT 알뜰폰 · 5 LG U+ 알뜰폰
     */
    private static final Map<String, String> TELECOM_CODES = Map.of(
            "SKT", "0",
            "KT", "1",
            "LGU", "2",
            "SKT_MVNO", "3",
            "KT_MVNO", "4",
            "LGU_MVNO", "5");

    static String telecomCode(String carrier) {
        String code = carrier == null ? null : TELECOM_CODES.get(carrier.trim().toUpperCase());
        if (code == null) {
            throw new BusinessException("INVALID_CREDENTIALS", "통신사를 선택해 주세요.");
        }
        return code;
    }

    /**
     * 생년월일 6자리 → 8자리(yyyyMMdd).
     *
     * 화면은 6자리로 받는다(사용자에게 익숙한 형식). 세기는 앞 두 자리로 가른다 —
     * `00~29` 는 2000년대, 그 외는 1900년대. 통상적인 관례이며,
     * 이 서비스 사용자층(MZ)에서 오판정이 날 구간은 사실상 없다.
     * 이미 8자리로 오면 그대로 둔다.
     */
    static String toEightDigitBirthDate(String birthDate) {
        String digits = digitsOnly(birthDate);
        if (digits.length() == 8) {
            return digits;
        }
        if (digits.length() != 6) {
            throw new BusinessException("INVALID_CREDENTIALS", "생년월일 6자리를 입력해 주세요.");
        }
        int year = Integer.parseInt(digits.substring(0, 2));
        String century = year <= 29 ? "20" : "19";
        return century + digits;
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private static Long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

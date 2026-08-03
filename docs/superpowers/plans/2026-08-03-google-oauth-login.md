# 구글 OAuth 로그인 (이슈 #9) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 구글 계정으로 로그인해 홈에 진입하고, 새로고침해도 세션이 유지되며, 미로그인 사용자는 모든 화면에서 로그인으로 튕기는 상태를 만든다.

**Architecture:** 백엔드 리다이렉트 Authorization Code 방식. 프론트는 `/api/auth/google`로 전체 이동만 하고, 백엔드가 구글과 code↔token 교환을 마친 뒤 리프레시 토큰을 httpOnly 쿠키로 심고 프론트로 되돌린다. 액세스 토큰(JWT 15분)은 Pinia 메모리에만 두고, 리프레시 토큰(14일)은 SHA-256 해시만 DB에 저장하며 사용할 때마다 회전한다. 인증 강제는 Spring Security 없이 `HandlerInterceptor` + `@LoginUser` ArgumentResolver로 직접 구현한다.

**Tech Stack:** Java 17 · Spring Framework 5.3.39 (Spring MVC, **Boot 아님**) · MyBatis 3.5.16 · MySQL 8 · Gradle · jjwt 0.12.6 / Vue 3 + Vite · Pinia · Vue Router · axios

**설계 원본:** `docs/superpowers/specs/2026-08-03-google-oauth-login-design.md`

---

## Global Constraints

모든 태스크의 요구사항에 아래가 암묵적으로 포함된다.

- **금지 기술 — 등장하면 즉시 중단하고 대체한다.** Spring Boot(`spring-boot-*`, `@SpringBootApplication`) 금지 → Spring Legacy(Spring MVC). JPA/Hibernate(`@Entity`, `JpaRepository`) 금지 → MyBatis. React 금지 → Vue3.
- **Java 들여쓰기 4칸.** 클래스 `PascalCase`, 메서드·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`.
- **JS/Vue 들여쓰기 4칸 · 세미콜론 사용 · 문자열은 싱글 쿼트.** Vue는 `<script setup>` + Composition API만. Options API 금지.
- **Vue 컴포넌트 파일명은 PascalCase이면서 두 단어 이상.** 페이지는 `views/`, 조각은 `components/`.
- **모든 REST 응답은 `com.kb.tangtang.common.dto.ApiResponse`로 감싼다.** raw 객체 반환 금지. 성공 `ApiResponse.ok(data)`, 실패 `ApiResponse.error(code, message)`.
- **업무 규칙 위반은 `BusinessException(code, message)`** → `CommonExceptionAdvice`가 400으로 변환. 컨트롤러에서 try-catch로 예외를 삼키지 않는다.
- **MyBatis 매퍼 인터페이스에는 반드시 `@Mapper`(`org.apache.ibatis.annotations.Mapper`)를 붙인다.** `@MapperScan(annotationClass = Mapper.class)`로 제한돼 있어 없으면 등록되지 않는다.
- **매퍼 XML에서 `${}` 금지, `#{}` 만 사용한다** (SQL Injection).
- **매퍼 XML의 `resultType`·`parameterType`은 FQCN으로 쓴다.** `mybatis-config.xml`에 `<typeAliases>`가 없다(Tomcat WebappClassLoader 충돌로 의도적으로 뺐다). 별칭을 추가하지 말 것.
- **매퍼 XML 위치는 `apps/api/src/main/resources/mapper/<모듈>/*.xml`.**
- **트랜잭션 경계는 Service에만.** Controller·Mapper에 `@Transactional` 금지.
- **`ServletConfig`에 `@Configuration`을 붙이지 말 것.** 붙이면 루트 컨텍스트에도 등록돼 `@EnableWebMvc`가 두 번 적용된다.
- **시크릿은 `application-local.properties`에만.** `application.properties`(커밋됨)에 계정·키를 쓰지 않는다. `.example` 파일에는 플레이스홀더만.
- **실제 DB 연결이 필요한 테스트는 `@Disabled`로 두고 커밋한다.** 활성화한 채 커밋하면 팀원 빌드가 깨진다.
- **CSS는 `apps/web/src/assets/tokens.css`의 의미 토큰만 참조한다. 색상 HEX 하드코딩 금지.** 원시 팔레트(`--tt-brand-700` 등)는 `tokens.css` 내부에서만 쓴다. `z-index`는 `--tt-z-*` 토큰만.
- **커밋 메시지는 한국어 `type: 내용`** (feat/fix/docs/refactor/test/chore/style/design).
- **브랜치는 `feature/9-auth-google-login`.** 이미 생성돼 있고 커밋 6건이 올라가 있다.
- **컴포넌트에서 `axios`를 직접 import 하지 않는다.** `src/api/http.js` 인스턴스를 쓴다. 단 `http.js` 자신은 예외.
- **`http.js` 응답 인터셉터가 `{success, data}` 래퍼를 이미 벗겨서 반환한다.** 호출부에서 `res.data.data` 같은 코드를 쓰지 않는다.

### 선행 조건 (이미 완료)

- `db/schema.sql` 33테이블 생성 완료, `db/seed.sql`로 `tbl_mission_difficulty` 3행(EASY=1/NORMAL=2/HARD=3) 적재 완료
- `apps/api/src/main/resources/application-local.properties` 작성 및 DB 접속 확인 완료

### 참조 스키마 (지어내지 말 것)

```sql
tbl_user(
  id BIGINT AI, social_provider VARCHAR(20) NOT NULL, provider_user_id VARCHAR(100) NOT NULL,
  email VARCHAR(100) NULL, nickname VARCHAR(50) NULL, name VARCHAR(50) NULL,
  phone VARCHAR(20) NULL, birth_date DATE NULL, gender VARCHAR(10) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',      -- ACTIVE/WITHDRAWN/BLOCKED
  withdrawn_at DATETIME NULL, difficulty_id BIGINT NOT NULL,   -- FK → tbl_mission_difficulty(id)
  created_at DATETIME, updated_at DATETIME,
  UNIQUE KEY uk_user_social (social_provider, provider_user_id))
-- 프로필 이미지 컬럼 없음. 구글 picture 클레임은 버린다.

tbl_refresh_token(
  id BIGINT AI, user_id BIGINT NOT NULL,           -- FK → tbl_user(id) ON DELETE CASCADE
  token_hash CHAR(64) NOT NULL,                     -- SHA-256 hex. 원문 미저장
  expires_at DATETIME NOT NULL,
  is_revoked TINYINT(1) NOT NULL DEFAULT 0, revoked_at DATETIME NULL,
  created_at DATETIME, updated_at DATETIME,
  UNIQUE KEY uk_rt_hash (token_hash))

tbl_user_consent(
  id BIGINT AI, user_id BIGINT NOT NULL, consent_type VARCHAR(30) NOT NULL,
  is_required TINYINT(1) NOT NULL, terms_version VARCHAR(20) NOT NULL,
  withdrawn_at DATETIME NULL, status TINYINT(1) NOT NULL, expires_at DATETIME NULL,
  UNIQUE KEY uk_consent_user_type (user_id, consent_type))
```

---

## File Structure

### 백엔드 (`apps/api`)

| 파일 | 책임 |
|---|---|
| `common/auth/JwtProvider.java` | 액세스 토큰 생성·검증. jjwt를 감싸는 유일한 지점 |
| `common/auth/LoginUser.java` | 파라미터 애너테이션 |
| `common/auth/LoginUserArgumentResolver.java` | request attribute → `Long userId` 주입 |
| `common/auth/JwtAuthInterceptor.java` | `Authorization: Bearer` 검증, 실패 시 401 JSON |
| `user/dto/UserDto.java` | `tbl_user` 행 매핑 |
| `user/dto/RefreshTokenDto.java` | `tbl_refresh_token` 행 매핑 |
| `user/dto/GoogleProfileDto.java` | 구글 id_token에서 뽑은 sub/email/name |
| `user/dto/UserMeDto.java` | `/api/users/me` 응답 |
| `user/dto/LoginResponseDto.java` | `/api/auth/refresh` 응답 |
| `user/mapper/UserMapper.java` + `mapper/user/UserMapper.xml` | 사용자 조회·삽입·동의 건수 |
| `user/mapper/RefreshTokenMapper.java` + `mapper/user/RefreshTokenMapper.xml` | 토큰 삽입·조회·폐기 |
| `user/service/GoogleOAuthClient.java` | 구글 authorization URL 생성, code→profile 교환 |
| `user/service/RefreshTokenService.java` | 발급·소비(회전)·재사용 감지 |
| `user/service/AuthService.java` | 로그인 오케스트레이션. 트랜잭션 경계 |
| `user/controller/AuthController.java` | 엔드포인트 4개 + 쿠키 |
| `user/controller/UserController.java` | `/api/users/me` |
| `config/ServletConfig.java` (수정) | 인터셉터·리졸버 등록 |

### 프론트 (`apps/web`)

| 파일 | 책임 |
|---|---|
| `stores/auth.js` | 액세스 토큰(메모리)·사용자·needsConsent |
| `api/auth.js` | 인증 API 호출 함수 |
| `api/http.js` (수정) | Bearer 주입 + 401 단일 재발급 재시도 |
| `views/auth/LoginView.vue` | 로그인 화면 |
| `views/auth/AuthCallbackView.vue` | 콜백 착지 → 분기 |
| `components/auth/GoogleSignInButton.vue` | 구글 버튼 |
| `assets/images/google-logo.svg` | 구글 G 로고 |
| `router/index.js` (수정) | 라우트 2개 + 가드 |
| `App.vue` (수정) | 탭바 조건부 렌더 |
| `main.js` (수정) | 부팅 시 세션 복원 |

> **팀 공유 파일 4개 수정**: `config/ServletConfig.java`, `api/http.js`, `router/index.js`, `App.vue`, `main.js`. PR 전에 팀에 알린다.

---

## Task 1: JwtProvider · 의존성 · 프로퍼티

**Files:**
- Modify: `apps/api/build.gradle`
- Modify: `apps/api/src/main/resources/application.properties`
- Modify: `apps/api/src/main/resources/application-local.properties.example`
- Create: `apps/api/src/main/java/com/kb/tangtang/common/auth/JwtProvider.java`
- Test: `apps/api/src/test/java/com/kb/tangtang/common/auth/JwtProviderTest.java`

**Interfaces:**
- Consumes: `com.kb.tangtang.common.exception.BusinessException(String code, String message)`
- Produces:
  - `JwtProvider(String secret, long accessTokenValiditySeconds)` — 생성자 직접 호출로 단위 테스트 가능
  - `String createAccessToken(Long userId)`
  - `Long parseUserId(String token)` — 실패 시 `BusinessException("TOKEN_EXPIRED"|"INVALID_TOKEN", …)`

- [ ] **Step 1: 의존성 추가**

`apps/api/build.gradle`의 `dependencies` 블록에서 `// ---- JSON ----` 바로 위에 추가한다.

```gradle
    // ---- JWT (jjwt) — Spring Boot 스타터가 아니므로 금지 기술이 아니다 ----
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

그리고 `// ---- Test ----` 블록의 마지막(`testRuntimeOnly` 줄 아래)에 추가한다.

```gradle
    testImplementation 'org.mockito:mockito-core:5.12.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.12.0'
    // 서블릿 API 는 main 이 compileOnly 라 테스트 클래스패스에 자동으로 오지 않는다.
    // MockHttpServletRequest 등을 쓰려면 테스트에도 따로 얹어야 한다.
    testCompileOnly 'javax.servlet:javax.servlet-api:4.0.1'
```

- [ ] **Step 2: 공통 프로퍼티 추가**

`apps/api/src/main/resources/application.properties` 끝에 추가한다. **시크릿은 넣지 않는다.**

```properties

# ── JWT ────────────────────────────────────────────────
# 액세스 토큰 15분 / 리프레시 토큰 14일 (초 단위)
jwt.access-token-validity=900
jwt.refresh-token-validity=1209600
```

- [ ] **Step 3: 개인 프로퍼티 예시에 jwt.secret 추가**

`apps/api/src/main/resources/application-local.properties.example` 끝에 추가한다.

```properties

# ── JWT 서명키 ─────────────────────────────────────────
# HS256 은 256비트 이상을 요구한다. 32자 미만이면 기동 시 WeakKeyException 으로 실패한다.
# 팀 개발용으로는 아무 랜덤 문자열 32자 이상이면 된다. 운영 값과 다르게 둔다.
jwt.secret=CHANGE_ME_JWT_SECRET_AT_LEAST_32_CHARS
```

- [ ] **Step 4: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/common/auth/JwtProviderTest.java`

```java
package com.kb.tangtang.common.auth;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtProvider 단위 테스트. Spring 컨텍스트도 DB 도 필요 없다.
 */
class JwtProviderTest {

    private static final String SECRET = "tangtang-test-secret-key-must-be-32-bytes-or-longer";

    @Test
    @DisplayName("발급한 토큰에서 userId 를 다시 꺼낸다")
    void createAndParse() {
        JwtProvider provider = new JwtProvider(SECRET, 900);

        String token = provider.createAccessToken(42L);

        assertEquals(42L, provider.parseUserId(token));
    }

    @Test
    @DisplayName("만료된 토큰은 TOKEN_EXPIRED 로 거부한다")
    void expiredToken() {
        // 유효기간을 음수로 주면 발급 시점에 이미 만료된 토큰이 나온다
        JwtProvider expiredProvider = new JwtProvider(SECRET, -60);
        String token = expiredProvider.createAccessToken(42L);

        JwtProvider provider = new JwtProvider(SECRET, 900);
        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId(token));

        assertEquals("TOKEN_EXPIRED", ex.getCode());
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 INVALID_TOKEN 으로 거부한다")
    void forgedSignature() {
        JwtProvider attacker = new JwtProvider("attacker-secret-key-that-is-also-32-bytes-long!!", 900);
        String forged = attacker.createAccessToken(42L);

        JwtProvider provider = new JwtProvider(SECRET, 900);
        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId(forged));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("형식이 깨진 문자열은 INVALID_TOKEN 으로 거부한다")
    void malformedToken() {
        JwtProvider provider = new JwtProvider(SECRET, 900);

        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId("not-a-jwt"));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }
}
```

- [ ] **Step 5: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*JwtProviderTest*"`
Expected: 컴파일 실패 — `JwtProvider` 심볼을 찾을 수 없음

- [ ] **Step 6: JwtProvider 구현**

`apps/api/src/main/java/com/kb/tangtang/common/auth/JwtProvider.java`

```java
package com.kb.tangtang.common.auth;

import com.kb.tangtang.common.exception.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 액세스 토큰(JWT) 발급·검증. jjwt 를 감싸는 유일한 지점이다.
 *
 * 생성자 주입만 쓰므로 Spring 없이 new 로 만들어 단위 테스트할 수 있다.
 * 서명키는 application-local.properties 의 jwt.secret (커밋 금지).
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-validity}") long accessTokenValiditySeconds) {
        // HS256 은 256비트 이상을 요구한다. 짧으면 여기서 WeakKeyException 이 나며 기동이 실패한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenValiditySeconds * 1000L))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 userId 를 꺼낸다.
     * 만료는 TOKEN_EXPIRED, 그 밖의 모든 실패(서명 위조·형식 오류·null)는 INVALID_TOKEN 이다.
     * 호출자(JwtAuthInterceptor)가 잡아서 401 로 변환한다.
     */
    public Long parseUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException("TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*JwtProviderTest*"`
Expected: PASS (4개 테스트)

- [ ] **Step 8: 커밋**

```bash
git add apps/api/build.gradle apps/api/src/main/resources/application.properties \
        apps/api/src/main/resources/application-local.properties.example \
        apps/api/src/main/java/com/kb/tangtang/common/auth/JwtProvider.java \
        apps/api/src/test/java/com/kb/tangtang/common/auth/JwtProviderTest.java
git commit -m "feat: JWT 액세스 토큰 발급·검증 JwtProvider 추가"
```

---

## Task 2: 사용자·토큰 DTO 와 MyBatis 매퍼

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/user/dto/UserDto.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/dto/RefreshTokenDto.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/mapper/UserMapper.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/mapper/RefreshTokenMapper.java`
- Create: `apps/api/src/main/resources/mapper/user/UserMapper.xml`
- Create: `apps/api/src/main/resources/mapper/user/RefreshTokenMapper.xml`
- Test: `apps/api/src/test/java/com/kb/tangtang/user/mapper/UserMapperTest.java`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `UserDto` — 필드 `Long id, String socialProvider, String providerUserId, String email, String nickname, String name, String status, Long difficultyId`. Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
  - `RefreshTokenDto` — 필드 `Long id, Long userId, String tokenHash, LocalDateTime expiresAt, boolean revoked, LocalDateTime revokedAt`
  - `UserMapper.findBySocialId(String socialProvider, String providerUserId) → UserDto|null`
  - `UserMapper.insert(UserDto user) → void` (실행 후 `user.getId()`에 생성 PK가 채워진다)
  - `UserMapper.countActiveConsents(Long userId) → int`
  - `RefreshTokenMapper.insert(RefreshTokenDto token) → void`
  - `RefreshTokenMapper.findByHash(String tokenHash) → RefreshTokenDto|null`
  - `RefreshTokenMapper.revokeById(Long id) → void`
  - `RefreshTokenMapper.revokeAllByUserId(Long userId) → void`

> `RefreshTokenDto`의 `is_revoked` 컬럼은 자바 필드명 `revoked`로 받는다. `mapUnderscoreToCamelCase=true`가 `is_revoked` → `isRevoked`로 매핑하므로 **XML에서 `AS revoked` 별칭을 명시**한다. 이름을 `isRevoked` 필드로 두면 Lombok이 `isRevoked()`/`setRevoked()`를 만들어 혼선이 생긴다.

- [ ] **Step 1: UserDto 작성**

`apps/api/src/main/java/com/kb/tangtang/user/dto/UserDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * tbl_user 행 매핑.
 * 프로필 이미지 컬럼은 스키마에 없다. 구글 picture 클레임은 저장하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String socialProvider;   // 'GOOGLE'
    private String providerUserId;   // 구글 sub
    private String email;
    private String nickname;         // 구글 name 을 넣는다
    private String name;             // 실명. 가입 시점엔 null (계좌 인증 단계에서 채움)
    private String status;           // ACTIVE / WITHDRAWN / BLOCKED
    private Long difficultyId;       // 가입 시 EASY(1)
}
```

- [ ] **Step 2: RefreshTokenDto 작성**

`apps/api/src/main/java/com/kb/tangtang/user/dto/RefreshTokenDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * tbl_refresh_token 행 매핑.
 * 원문 토큰은 저장하지 않는다. tokenHash 는 SHA-256 hex 64자.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenDto {

    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean revoked;          // 컬럼 is_revoked. XML 에서 AS revoked 로 매핑한다
    private LocalDateTime revokedAt;
}
```

- [ ] **Step 3: 매퍼 인터페이스 2개 작성**

`apps/api/src/main/java/com/kb/tangtang/user/mapper/UserMapper.java`

```java
package com.kb.tangtang.user.mapper;

import com.kb.tangtang.user.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Mapper 는 필수다. RootConfig 가 @MapperScan(annotationClass = Mapper.class) 로
 * 제한하고 있어 붙이지 않으면 빈으로 등록되지 않는다.
 */
@Mapper
public interface UserMapper {

    UserDto findBySocialId(@Param("socialProvider") String socialProvider,
                           @Param("providerUserId") String providerUserId);

    /** 실행 후 user.getId() 에 생성된 PK 가 채워진다. */
    void insert(UserDto user);

    /**
     * 철회하지 않은 필수 동의 건수. 0 이면 아직 동의 절차를 밟지 않은 사용자다.
     * 동의 화면은 후속 이슈지만, needsConsent 를 가짜 값으로 두지 않기 위해 여기서 조회한다.
     */
    int countActiveConsents(@Param("userId") Long userId);
}
```

`apps/api/src/main/java/com/kb/tangtang/user/mapper/RefreshTokenMapper.java`

```java
package com.kb.tangtang.user.mapper;

import com.kb.tangtang.user.dto.RefreshTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

    void insert(RefreshTokenDto token);

    /** 폐기 여부와 무관하게 조회한다. 재사용 감지를 하려면 폐기된 행도 보여야 한다. */
    RefreshTokenDto findByHash(@Param("tokenHash") String tokenHash);

    void revokeById(@Param("id") Long id);

    /** 탈취 의심 시 해당 사용자의 살아 있는 토큰을 전부 폐기한다. */
    void revokeAllByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 4: 매퍼 XML 2개 작성**

`apps/api/src/main/resources/mapper/user/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!--
  typeAliases 를 쓰지 않는 프로젝트다. resultType / parameterType 은 FQCN 으로 쓴다.
  파라미터는 #{} 만 사용한다. ${} 는 SQL Injection 위험으로 금지.
-->
<mapper namespace="com.kb.tangtang.user.mapper.UserMapper">

    <!-- 조회 컬럼은 한 곳에서만 정의한다. 컬럼이 늘어도 고칠 데가 하나다. -->
    <sql id="userColumns">
        id, social_provider, provider_user_id, email, nickname, name, status, difficulty_id
    </sql>

    <select id="findBySocialId" resultType="com.kb.tangtang.user.dto.UserDto">
        SELECT <include refid="userColumns"/>
          FROM tbl_user
         WHERE social_provider  = #{socialProvider}
           AND provider_user_id = #{providerUserId}
    </select>

    <insert id="insert"
            parameterType="com.kb.tangtang.user.dto.UserDto"
            useGeneratedKeys="true"
            keyProperty="id">
        INSERT INTO tbl_user (social_provider, provider_user_id, email, nickname, status, difficulty_id)
        VALUES (#{socialProvider}, #{providerUserId}, #{email}, #{nickname}, #{status}, #{difficultyId})
    </insert>

    <select id="countActiveConsents" resultType="int">
        SELECT COUNT(*)
          FROM tbl_user_consent
         WHERE user_id      = #{userId}
           AND is_required  = 1
           AND withdrawn_at IS NULL
    </select>

</mapper>
```

`apps/api/src/main/resources/mapper/user/RefreshTokenMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.kb.tangtang.user.mapper.RefreshTokenMapper">

    <insert id="insert"
            parameterType="com.kb.tangtang.user.dto.RefreshTokenDto"
            useGeneratedKeys="true"
            keyProperty="id">
        INSERT INTO tbl_refresh_token (user_id, token_hash, expires_at, is_revoked)
        VALUES (#{userId}, #{tokenHash}, #{expiresAt}, 0)
    </insert>

    <!-- is_revoked 를 AS revoked 로 내린다. 별칭이 없으면 isRevoked 로 매핑돼 DTO 필드와 어긋난다. -->
    <select id="findByHash" resultType="com.kb.tangtang.user.dto.RefreshTokenDto">
        SELECT id,
               user_id,
               token_hash,
               expires_at,
               is_revoked AS revoked,
               revoked_at
          FROM tbl_refresh_token
         WHERE token_hash = #{tokenHash}
    </select>

    <update id="revokeById">
        UPDATE tbl_refresh_token
           SET is_revoked = 1,
               revoked_at = NOW()
         WHERE id = #{id}
    </update>

    <update id="revokeAllByUserId">
        UPDATE tbl_refresh_token
           SET is_revoked = 1,
               revoked_at = NOW()
         WHERE user_id    = #{userId}
           AND is_revoked = 0
    </update>

</mapper>
```

- [ ] **Step 5: 실DB 통합 테스트 작성 (`@Disabled` 로 커밋)**

`apps/api/src/test/java/com/kb/tangtang/user/mapper/UserMapperTest.java`

```java
package com.kb.tangtang.user.mapper;

import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.dto.UserDto;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 매퍼 XML 이 실제 스키마와 맞는지 확인하는 통합 테스트.
 *
 * 실제 MySQL 연결이 필요하므로 기본은 비활성화다.
 * 매퍼를 고친 뒤 한 번씩 @Disabled 를 주석 처리하고 돌려 확인한 다음,
 * 반드시 다시 활성화해서 커밋한다. (켜둔 채 커밋하면 팀원 빌드가 깨진다)
 */
@Disabled("실DB 연결이 필요할 때만 임시로 해제")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
@Rollback
@Log4j2
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    @DisplayName("사용자를 넣고 소셜 ID 로 다시 찾는다")
    void insertAndFind() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE")
                .providerUserId("test-sub-0001")
                .email("test@example.com")
                .nickname("테스트지윤")
                .status("ACTIVE")
                .difficultyId(1L)
                .build();

        userMapper.insert(user);
        assertNotNull(user.getId(), "useGeneratedKeys 가 PK 를 채워야 한다");

        UserDto found = userMapper.findBySocialId("GOOGLE", "test-sub-0001");
        assertNotNull(found);
        assertEquals("테스트지윤", found.getNickname());
        assertEquals("ACTIVE", found.getStatus());
        assertEquals(1L, found.getDifficultyId());
    }

    @Test
    @DisplayName("없는 소셜 ID 는 null 을 돌려준다")
    void findMissing() {
        assertNull(userMapper.findBySocialId("GOOGLE", "no-such-sub"));
    }

    @Test
    @DisplayName("동의 이력이 없는 사용자는 0 건이다")
    void countConsentsEmpty() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0002")
                .nickname("동의없음").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        assertEquals(0, userMapper.countActiveConsents(user.getId()));
    }

    @Test
    @DisplayName("리프레시 토큰을 넣고 해시로 찾은 뒤 폐기한다")
    void refreshTokenLifecycle() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0003")
                .nickname("토큰유저").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        RefreshTokenDto token = RefreshTokenDto.builder()
                .userId(user.getId())
                .tokenHash("a".repeat(64))
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
        refreshTokenMapper.insert(token);

        RefreshTokenDto found = refreshTokenMapper.findByHash("a".repeat(64));
        assertNotNull(found);
        assertEquals(user.getId(), found.getUserId());
        assertTrue(!found.isRevoked(), "새로 발급한 토큰은 폐기 상태가 아니어야 한다");

        refreshTokenMapper.revokeById(found.getId());

        RefreshTokenDto revoked = refreshTokenMapper.findByHash("a".repeat(64));
        assertTrue(revoked.isRevoked(), "폐기 후에도 조회는 되어야 재사용 감지가 가능하다");
    }
}
```

- [ ] **Step 6: 컴파일 확인**

Run: `./gradlew :apps:api:build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 매퍼를 실DB로 한 번 검증**

`UserMapperTest.java`의 `@Disabled` 줄 맨 앞에 `//` 를 붙여 임시 해제한 뒤 실행한다.

Run: `./gradlew :apps:api:test --tests "*UserMapperTest*"`
Expected: PASS (4개). 실패하면 XML 컬럼명을 `db/schema.sql`과 대조한다.

확인 후 **`@Disabled` 를 반드시 되돌린다.**

Run: `./gradlew :apps:api:test`
Expected: `UserMapperTest` 가 skipped 로 표시된다

- [ ] **Step 8: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/user/ \
        apps/api/src/main/resources/mapper/user/ \
        apps/api/src/test/java/com/kb/tangtang/user/
git commit -m "feat: 사용자·리프레시 토큰 DTO 와 MyBatis 매퍼 추가"
```

---

## Task 3: RefreshTokenService — 발급 · 회전 · 재사용 감지

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/user/service/RefreshTokenService.java`
- Test: `apps/api/src/test/java/com/kb/tangtang/user/service/RefreshTokenServiceTest.java`

**Interfaces:**
- Consumes: `RefreshTokenMapper` (Task 2)
- Produces:
  - `RefreshTokenService(RefreshTokenMapper mapper, long refreshTokenValiditySeconds)`
  - `String issue(Long userId)` — 원문 토큰(UUID) 반환, 해시만 저장
  - `Long consume(String rawToken)` — 검증 + 폐기 + userId 반환. 재사용 시 전체 폐기 후 `BusinessException("REFRESH_TOKEN_REUSED", …)`
  - `void revoke(String rawToken)` — 로그아웃용. 없는 토큰이면 조용히 무시
  - `static String sha256Hex(String raw)`

- [ ] **Step 1: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/user/service/RefreshTokenServiceTest.java`

```java
package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        // 리프레시 토큰 유효기간 14일(초)
        service = new RefreshTokenService(refreshTokenMapper, 1209600);
    }

    @Test
    @DisplayName("발급하면 원문을 돌려주고 DB 에는 해시만 저장한다")
    void issueStoresHashOnly() {
        String raw = service.issue(7L);

        ArgumentCaptor<RefreshTokenDto> captor = ArgumentCaptor.forClass(RefreshTokenDto.class);
        verify(refreshTokenMapper).insert(captor.capture());
        RefreshTokenDto saved = captor.getValue();

        assertEquals(7L, saved.getUserId());
        assertEquals(64, saved.getTokenHash().length(), "SHA-256 hex 는 64자다");
        assertNotEquals(raw, saved.getTokenHash(), "원문을 그대로 저장하면 안 된다");
        assertEquals(RefreshTokenService.sha256Hex(raw), saved.getTokenHash());
    }

    @Test
    @DisplayName("정상 토큰을 소비하면 폐기하고 userId 를 돌려준다")
    void consumeRevokesAndReturnsUserId() {
        String raw = "raw-token-value";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .revoked(false)
                        .build());

        Long userId = service.consume(raw);

        assertEquals(7L, userId);
        verify(refreshTokenMapper).revokeById(100L);
        verify(refreshTokenMapper, never()).revokeAllByUserId(7L);
    }

    @Test
    @DisplayName("이미 폐기된 토큰이 다시 오면 전체 폐기하고 REFRESH_TOKEN_REUSED 를 던진다")
    void consumeDetectsReuse() {
        String raw = "stolen-token";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .revoked(true)
                        .build());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(raw));

        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());
        verify(refreshTokenMapper).revokeAllByUserId(7L);
    }

    @Test
    @DisplayName("만료된 토큰은 INVALID_TOKEN 으로 거부한다")
    void consumeRejectsExpired() {
        String raw = "expired-token";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .revoked(false)
                        .build());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(raw));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 INVALID_TOKEN 으로 거부한다")
    void consumeRejectsUnknown() {
        when(refreshTokenMapper.findByHash(anyString())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume("nope"));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("null 토큰은 INVALID_TOKEN 으로 거부한다 — 쿠키가 없는 경우")
    void consumeRejectsNull() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(null));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("로그아웃은 없는 토큰이어도 조용히 넘어간다")
    void revokeIgnoresUnknown() {
        when(refreshTokenMapper.findByHash(anyString())).thenReturn(null);

        service.revoke("nope");

        verify(refreshTokenMapper, never()).revokeById(org.mockito.ArgumentMatchers.anyLong());
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*RefreshTokenServiceTest*"`
Expected: 컴파일 실패 — `RefreshTokenService` 심볼을 찾을 수 없음

- [ ] **Step 3: RefreshTokenService 구현**

`apps/api/src/main/java/com/kb/tangtang/user/service/RefreshTokenService.java`

```java
package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 리프레시 토큰 발급 · 회전 · 재사용 감지.
 *
 * 원문 토큰은 DB 에 저장하지 않는다. SHA-256 해시만 저장하고, 검증할 때 다시 해시해 비교한다.
 * (tbl_refresh_token.token_hash 컬럼 주석의 설계 의도)
 *
 * 회전: consume() 이 기존 토큰을 폐기하고 userId 만 돌려준다.
 *       호출자가 이어서 issue() 로 새 토큰을 받는다.
 * 재사용 감지: 이미 폐기된 토큰이 다시 들어오면 탈취로 간주하고 해당 사용자 토큰을 전부 폐기한다.
 */
@Service
@Log4j2
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final long refreshTokenValiditySeconds;

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper,
                               @Value("${jwt.refresh-token-validity}") long refreshTokenValiditySeconds) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    /** 새 토큰을 발급한다. 반환값은 쿠키에 실어 보낼 원문이며, DB 에는 해시만 남는다. */
    public String issue(Long userId) {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshTokenMapper.insert(RefreshTokenDto.builder()
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds))
                .build());
        return raw;
    }

    /**
     * 토큰을 검증하고 폐기한 뒤 userId 를 돌려준다.
     * 이미 폐기된 토큰이면 탈취로 보고 해당 사용자의 전체 토큰을 폐기한다.
     */
    public Long consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("INVALID_TOKEN", "리프레시 토큰이 없습니다.");
        }

        RefreshTokenDto token = refreshTokenMapper.findByHash(sha256Hex(rawToken));
        if (token == null) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
        }

        if (token.isRevoked()) {
            // 폐기된 토큰이 다시 왔다 = 누군가 탈취본을 쓰고 있다는 신호
            log.warn("리프레시 토큰 재사용 감지 — userId={} 전체 폐기", token.getUserId());
            refreshTokenMapper.revokeAllByUserId(token.getUserId());
            throw new BusinessException("REFRESH_TOKEN_REUSED",
                    "보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.");
        }

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("INVALID_TOKEN", "리프레시 토큰이 만료되었습니다.");
        }

        refreshTokenMapper.revokeById(token.getId());
        return token.getUserId();
    }

    /** 로그아웃. 이미 없거나 폐기된 토큰이어도 오류로 만들지 않는다. */
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshTokenDto token = refreshTokenMapper.findByHash(sha256Hex(rawToken));
        if (token != null && !token.isRevoked()) {
            refreshTokenMapper.revokeById(token.getId());
        }
    }

    /** SHA-256 hex 64자. CHAR(64) 컬럼과 길이가 맞는다. */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 은 모든 JVM 이 반드시 지원한다. 여기 오면 환경이 깨진 것이다.
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", ex);
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*RefreshTokenServiceTest*"`
Expected: PASS (7개 테스트)

- [ ] **Step 5: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/user/service/RefreshTokenService.java \
        apps/api/src/test/java/com/kb/tangtang/user/service/RefreshTokenServiceTest.java
git commit -m "feat: 리프레시 토큰 회전과 재사용 감지 구현"
```

---

## Task 4: GoogleOAuthClient — authorization URL · code 교환

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/user/dto/GoogleProfileDto.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/service/GoogleOAuthClient.java`
- Modify: `apps/api/src/main/resources/application.properties`
- Modify: `apps/api/src/main/resources/application-local.properties.example`
- Test: `apps/api/src/test/java/com/kb/tangtang/user/service/GoogleOAuthClientTest.java`

**Interfaces:**
- Consumes: `BusinessException`
- Produces:
  - `GoogleProfileDto` — 필드 `String providerUserId, String email, String name`
  - `GoogleOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper, String clientId, String clientSecret, String redirectUri, String authorizationUri, String tokenUri)`
  - `String buildAuthorizationUrl(String state)`
  - `GoogleProfileDto exchangeCodeForProfile(String code)`

- [ ] **Step 1: 프로퍼티 추가**

`apps/api/src/main/resources/application.properties` 끝에 추가한다.

```properties

# ── Google OAuth (시크릿 아닌 값만) ─────────────────────
google.oauth.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
google.oauth.token-uri=https://oauth2.googleapis.com/token
# 리다이렉트 URI 는 프론트 오리진(:5173) 기준이어야 한다.
# Vite 프록시를 거쳐야 리프레시 쿠키가 프론트와 same-origin 으로 심긴다.
# Google Cloud Console 의 "승인된 리디렉션 URI" 에도 똑같이 등록돼 있어야 한다.
google.oauth.redirect-uri=http://localhost:5173/api/auth/google/callback

# ── 프론트 주소 (콜백 후 되돌려보낼 곳) ─────────────────
app.front-url=http://localhost:5173

# ── 리프레시 쿠키 속성 ─────────────────────────────────
# 로컬은 Vite 프록시로 same-origin 이라 Lax 로 충분하다.
# 프론트와 API 도메인이 갈리는 환경에서는 None + secure=true 로 덮어쓴다.
# (SameSite=None 은 Secure 없이는 브라우저가 쿠키를 버린다)
auth.cookie.same-site=Lax
auth.cookie.secure=false
```

- [ ] **Step 2: 개인 프로퍼티 예시에 구글 시크릿 추가**

`apps/api/src/main/resources/application-local.properties.example` 끝에 추가한다.

```properties

# ── Google OAuth 클라이언트 (커밋 금지) ────────────────
# Google Cloud Console > API 및 서비스 > 사용자 인증 정보 에서 발급.
# 팀 개발용 클라이언트 하나를 공유한다. 값은 팀 채널에서 받는다.
google.oauth.client-id=CHANGE_ME_GOOGLE_CLIENT_ID
google.oauth.client-secret=CHANGE_ME_GOOGLE_CLIENT_SECRET
```

- [ ] **Step 3: GoogleProfileDto 작성**

`apps/api/src/main/java/com/kb/tangtang/user/dto/GoogleProfileDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구글 id_token 에서 뽑아낸 최소 프로필.
 * picture 클레임은 저장할 컬럼이 없으므로 받지 않는다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleProfileDto {

    private String providerUserId;   // sub — 구글 계정의 불변 고유 ID
    private String email;
    private String name;
}
```

- [ ] **Step 4: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/user/service/GoogleOAuthClientTest.java`

```java
package com.kb.tangtang.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 네트워크를 타지 않는다. MockRestServiceServer 로 구글 토큰 엔드포인트를 흉내낸다.
 */
class GoogleOAuthClientTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String REDIRECT_URI = "http://localhost:5173/api/auth/google/callback";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GoogleOAuthClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new GoogleOAuthClient(restTemplate, new ObjectMapper(),
                "test-client-id", "test-client-secret", REDIRECT_URI, AUTH_URI, TOKEN_URI);
    }

    /** 구글 id_token 흉내 — 헤더.페이로드.서명 세 토막. 페이로드만 base64url 로 읽는다. */
    private static String fakeIdToken(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fake-signature";
    }

    @Test
    @DisplayName("authorization URL 에 필수 파라미터가 모두 들어간다")
    void buildAuthorizationUrl() {
        String url = client.buildAuthorizationUrl("state-abc");

        assertTrue(url.startsWith(AUTH_URI + "?"), "구글 인가 엔드포인트로 시작해야 한다");
        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state=state-abc"));
        assertTrue(url.contains("scope=openid+email+profile")
                        || url.contains("scope=openid%20email%20profile"),
                "openid email profile 스코프가 있어야 한다");
        assertTrue(url.contains("redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Fapi%2Fauth%2Fgoogle%2Fcallback"),
                "redirect_uri 는 URL 인코딩돼야 한다");
    }

    @Test
    @DisplayName("code 를 교환해 id_token 에서 프로필을 뽑는다")
    void exchangeCodeForProfile() {
        String idToken = fakeIdToken(
                "{\"sub\":\"1234567890\",\"email\":\"jiyoon@example.com\",\"name\":\"지윤\"}");
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"id_token\":\"" + idToken + "\"}",
                        MediaType.APPLICATION_JSON));

        GoogleProfileDto profile = client.exchangeCodeForProfile("auth-code");

        assertEquals("1234567890", profile.getProviderUserId());
        assertEquals("jiyoon@example.com", profile.getEmail());
        assertEquals("지윤", profile.getName());
        mockServer.verify();
    }

    @Test
    @DisplayName("구글이 오류를 주면 OAUTH_TOKEN_EXCHANGE_FAILED 로 바꾼다")
    void exchangeFails() {
        mockServer.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("bad-code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }

    @Test
    @DisplayName("id_token 이 없는 응답도 OAUTH_TOKEN_EXCHANGE_FAILED 로 바꾼다")
    void missingIdToken() {
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"only-access\"}", MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }

    @Test
    @DisplayName("sub 가 없는 id_token 은 거부한다")
    void missingSub() {
        String idToken = fakeIdToken("{\"email\":\"no-sub@example.com\"}");
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"id_token\":\"" + idToken + "\"}", MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.exchangeCodeForProfile("code"));

        assertEquals("OAUTH_TOKEN_EXCHANGE_FAILED", ex.getCode());
    }
}
```

- [ ] **Step 5: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*GoogleOAuthClientTest*"`
Expected: 컴파일 실패 — `GoogleOAuthClient` 심볼을 찾을 수 없음

- [ ] **Step 6: GoogleOAuthClient 구현**

`apps/api/src/main/java/com/kb/tangtang/user/service/GoogleOAuthClient.java`

```java
package com.kb.tangtang.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 구글 OAuth 2.0 연동. 이 클래스만 구글과 통신한다.
 *
 * RestTemplate 은 생성자로 받는다 — 테스트에서 MockRestServiceServer 를 물리기 위해서다.
 */
@Component
@Log4j2
public class GoogleOAuthClient {

    private static final String SCOPE = "openid email profile";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizationUri;
    private final String tokenUri;

    public GoogleOAuthClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             @Value("${google.oauth.client-id}") String clientId,
                             @Value("${google.oauth.client-secret}") String clientSecret,
                             @Value("${google.oauth.redirect-uri}") String redirectUri,
                             @Value("${google.oauth.authorization-uri}") String authorizationUri,
                             @Value("${google.oauth.token-uri}") String tokenUri) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
    }

    /** 사용자를 보낼 구글 동의 화면 주소. state 는 호출자가 만들어 쿠키에도 저장한다. */
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    /**
     * authorization code 를 구글 토큰 엔드포인트에서 교환하고 id_token 의 클레임을 돌려준다.
     *
     * id_token 의 서명은 검증하지 않는다. TLS 로 구글 토큰 엔드포인트와 직접 통신해 받은
     * 응답이므로 출처가 보장된다(OpenID Connect Core 3.1.3.7 — 코드 플로우에서 서버가
     * 직접 받은 id_token 은 서명 검증 생략이 허용된다). 프론트에서 받은 id_token 을
     * 검증 없이 신뢰하는 것과는 다른 상황이다.
     */
    public GoogleProfileDto exchangeCodeForProfile(String code) {
        String idToken = requestIdToken(code);
        return parseIdToken(idToken);
    }

    private String requestIdToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body;
        try {
            body = restTemplate.postForObject(tokenUri, new HttpEntity<>(form, headers), String.class);
        } catch (RestClientException ex) {
            log.warn("구글 토큰 교환 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode idToken = root.get("id_token");
            if (idToken == null || idToken.asText().isBlank()) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
            }
            return idToken.asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("구글 토큰 응답 파싱 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }
    }

    private GoogleProfileDto parseIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));

            JsonNode sub = claims.get("sub");
            if (sub == null || sub.asText().isBlank()) {
                throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 계정 식별자를 확인할 수 없습니다.");
            }

            return GoogleProfileDto.builder()
                    .providerUserId(sub.asText())
                    .email(claims.hasNonNull("email") ? claims.get("email").asText() : null)
                    .name(claims.hasNonNull("name") ? claims.get("name").asText() : null)
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("id_token 파싱 실패", ex);
            throw new BusinessException("OAUTH_TOKEN_EXCHANGE_FAILED", "구글 인증에 실패했습니다.");
        }
    }
}
```

- [ ] **Step 7: RestTemplate · ObjectMapper 빈 등록**

`apps/api/src/main/java/com/kb/tangtang/config/RootConfig.java` 의 `transactionManager()` 메서드 아래에 추가한다. (import 는 IDE 가 채우거나 `org.springframework.web.client.RestTemplate`, `com.fasterxml.jackson.databind.ObjectMapper` 를 직접 추가)

```java
    /** 외부 API 호출용. 지금은 구글 OAuth 만 쓴다. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
```

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*GoogleOAuthClientTest*"`
Expected: PASS (5개 테스트)

- [ ] **Step 9: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/user/dto/GoogleProfileDto.java \
        apps/api/src/main/java/com/kb/tangtang/user/service/GoogleOAuthClient.java \
        apps/api/src/main/java/com/kb/tangtang/config/RootConfig.java \
        apps/api/src/main/resources/application.properties \
        apps/api/src/main/resources/application-local.properties.example \
        apps/api/src/test/java/com/kb/tangtang/user/service/GoogleOAuthClientTest.java
git commit -m "feat: 구글 OAuth 인가 URL 생성과 code 교환 클라이언트 추가"
```

---

## Task 5: AuthService — 로그인 오케스트레이션

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/user/dto/UserMeDto.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/dto/LoginResponseDto.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/service/AuthService.java`
- Test: `apps/api/src/test/java/com/kb/tangtang/user/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `GoogleOAuthClient` (Task 4), `RefreshTokenService` (Task 3), `UserMapper` (Task 2), `JwtProvider` (Task 1)
- Produces:
  - `UserMeDto` — 필드 `Long id, String nickname, String email`
  - `LoginResponseDto` — 필드 `String accessToken, UserMeDto user, boolean needsConsent`. **리프레시 토큰은 여기 담지 않는다** (JSON 본문에 나가면 안 되고 쿠키로만 나간다)
  - `AuthResultDto` — 필드 `LoginResponseDto response, String refreshToken`. 서비스가 컨트롤러에 둘을 한 번에 넘기기 위한 묶음이다. `response` 는 JSON 본문으로, `refreshToken` 은 `Set-Cookie` 로 나간다
  - `AuthService.loginWithGoogleCode(String code) → AuthResultDto`
  - `AuthService.refresh(String rawRefreshToken) → AuthResultDto`
  - `AuthService.logout(String rawRefreshToken) → void`
  - `AuthResultDto` — 필드 `LoginResponseDto response, String refreshToken`

- [ ] **Step 1: 응답 DTO 3개 작성**

`apps/api/src/main/java/com/kb/tangtang/user/dto/UserMeDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** GET /api/users/me 응답. tbl_user 에 프로필 이미지 컬럼이 없으므로 포함하지 않는다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMeDto {

    private Long id;
    private String nickname;
    private String email;
}
```

`apps/api/src/main/java/com/kb/tangtang/user/dto/LoginResponseDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/refresh 응답 본문.
 * 리프레시 토큰은 여기 담지 않는다 — httpOnly 쿠키로만 나간다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String accessToken;
    private UserMeDto user;
    /**
     * 필수 동의를 아직 하지 않은 사용자인지. 동의 화면은 후속 이슈라
     * 이번 범위에서는 프론트가 값을 저장만 하고 라우팅에 쓰지 않는다.
     */
    private boolean needsConsent;
}
```

`apps/api/src/main/java/com/kb/tangtang/user/dto/AuthResultDto.java`

```java
package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스 → 컨트롤러 전달용 묶음.
 * response 는 JSON 본문으로, refreshToken 은 Set-Cookie 로 나간다.
 */
@Getter
@AllArgsConstructor
@Builder
public class AuthResultDto {

    private LoginResponseDto response;
    private String refreshToken;
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/user/service/AuthServiceTest.java`

```java
package com.kb.tangtang.user.service;

import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private GoogleOAuthClient googleOAuthClient;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;
    @Mock private JwtProvider jwtProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(googleOAuthClient, refreshTokenService, userMapper, jwtProvider);
    }

    private static GoogleProfileDto profile() {
        return GoogleProfileDto.builder()
                .providerUserId("google-sub-1")
                .email("jiyoon@example.com")
                .name("지윤")
                .build();
    }

    @Test
    @DisplayName("처음 로그인하는 사용자는 EASY 난이도로 새로 만든다")
    void firstLoginCreatesUser() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(null);
        // insert 가 PK 를 채우는 동작을 흉내낸다
        doAnswer(invocation -> {
            UserDto arg = invocation.getArgument(0);
            arg.setId(11L);
            return null;
        }).when(userMapper).insert(any(UserDto.class));
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-raw");
        when(userMapper.countActiveConsents(11L)).thenReturn(0);

        AuthResultDto result = authService.loginWithGoogleCode("code");

        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(userMapper).insert(captor.capture());
        UserDto created = captor.getValue();
        assertEquals("GOOGLE", created.getSocialProvider());
        assertEquals("google-sub-1", created.getProviderUserId());
        assertEquals("지윤", created.getNickname());
        assertEquals("ACTIVE", created.getStatus());
        assertEquals(1L, created.getDifficultyId(), "가입 시 EASY(1) 를 부여한다");

        assertEquals("access-jwt", result.getResponse().getAccessToken());
        assertEquals("refresh-raw", result.getRefreshToken());
        assertEquals(11L, result.getResponse().getUser().getId());
        assertTrue(result.getResponse().isNeedsConsent(), "동의 이력이 없으면 true");
    }

    @Test
    @DisplayName("이미 있는 사용자는 새로 만들지 않는다")
    void existingUserIsReused() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com")
                        .status("ACTIVE").difficultyId(2L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-raw");
        when(userMapper.countActiveConsents(11L)).thenReturn(3);

        AuthResultDto result = authService.loginWithGoogleCode("code");

        verify(userMapper, never()).insert(any(UserDto.class));
        assertEquals("지윤", result.getResponse().getUser().getNickname());
        assertFalse(result.getResponse().isNeedsConsent(), "동의 이력이 있으면 false");
    }

    @Test
    @DisplayName("탈퇴한 사용자는 USER_WITHDRAWN 으로 막는다")
    void withdrawnUserRejected() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).status("WITHDRAWN").difficultyId(1L).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginWithGoogleCode("code"));

        assertEquals("USER_WITHDRAWN", ex.getCode());
        verify(jwtProvider, never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("차단된 사용자도 USER_WITHDRAWN 으로 막는다")
    void blockedUserRejected() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).status("BLOCKED").difficultyId(1L).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginWithGoogleCode("code"));

        assertEquals("USER_WITHDRAWN", ex.getCode());
    }

    @Test
    @DisplayName("refresh 는 기존 토큰을 소비하고 새 토큰 쌍을 만든다")
    void refreshRotates() {
        when(refreshTokenService.consume("old-raw")).thenReturn(11L);
        when(userMapper.findById(11L)).thenReturn(
                UserDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com")
                        .status("ACTIVE").difficultyId(1L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("new-access");
        when(refreshTokenService.issue(11L)).thenReturn("new-raw");
        when(userMapper.countActiveConsents(11L)).thenReturn(0);

        AuthResultDto result = authService.refresh("old-raw");

        verify(refreshTokenService).consume("old-raw");
        assertEquals("new-access", result.getResponse().getAccessToken());
        assertEquals("new-raw", result.getRefreshToken());
    }

    @Test
    @DisplayName("logout 은 토큰을 폐기한다")
    void logoutRevokes() {
        authService.logout("raw");

        verify(refreshTokenService).revoke("raw");
    }
}
```

> 이 테스트는 `UserMapper.findById(Long)` 를 요구한다. Task 2 에 없던 메서드이므로 Step 3 에서 매퍼에 추가한다.

- [ ] **Step 3: UserMapper 에 findById 추가**

`apps/api/src/main/java/com/kb/tangtang/user/mapper/UserMapper.java` 의 `countActiveConsents` 위에 추가한다.

```java
    UserDto findById(@Param("id") Long id);
```

`apps/api/src/main/resources/mapper/user/UserMapper.xml` 의 `countActiveConsents` 앞에 추가한다.

```xml
    <select id="findById" resultType="com.kb.tangtang.user.dto.UserDto">
        SELECT <include refid="userColumns"/>
          FROM tbl_user
         WHERE id = #{id}
    </select>
```

- [ ] **Step 4: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*AuthServiceTest*"`
Expected: 컴파일 실패 — `AuthService` 심볼을 찾을 수 없음

- [ ] **Step 5: AuthService 구현**

`apps/api/src/main/java/com/kb/tangtang/user/service/AuthService.java`

```java
package com.kb.tangtang.user.service;

import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 오케스트레이션. 트랜잭션 경계는 여기다(Controller·Mapper 에 두지 않는다).
 */
@Service
@Log4j2
public class AuthService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    /** 가입 시 부여하는 난이도. db/seed.sql 의 tbl_mission_difficulty EASY 행 id. */
    private static final long DEFAULT_DIFFICULTY_ID = 1L;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final GoogleOAuthClient googleOAuthClient;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;

    public AuthService(GoogleOAuthClient googleOAuthClient,
                       RefreshTokenService refreshTokenService,
                       UserMapper userMapper,
                       JwtProvider jwtProvider) {
        this.googleOAuthClient = googleOAuthClient;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.jwtProvider = jwtProvider;
    }

    /** 구글 콜백에서 받은 code 로 로그인/가입을 마치고 토큰 쌍을 만든다. */
    @Transactional
    public AuthResultDto loginWithGoogleCode(String code) {
        GoogleProfileDto profile = googleOAuthClient.exchangeCodeForProfile(code);

        UserDto user = userMapper.findBySocialId(PROVIDER_GOOGLE, profile.getProviderUserId());
        if (user == null) {
            user = createUser(profile);
        } else if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다.");
        }

        return buildResult(user);
    }

    /** 리프레시 토큰을 회전시키고 새 토큰 쌍을 만든다. */
    @Transactional
    public AuthResultDto refresh(String rawRefreshToken) {
        Long userId = refreshTokenService.consume(rawRefreshToken);

        UserDto user = userMapper.findById(userId);
        if (user == null || !STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다.");
        }

        return buildResult(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private UserDto createUser(GoogleProfileDto profile) {
        UserDto user = UserDto.builder()
                .socialProvider(PROVIDER_GOOGLE)
                .providerUserId(profile.getProviderUserId())
                .email(profile.getEmail())
                .nickname(profile.getName())   // 실명(name)은 계좌 인증 단계에서 채운다
                .status(STATUS_ACTIVE)
                .difficultyId(DEFAULT_DIFFICULTY_ID)
                .build();
        userMapper.insert(user);
        log.info("신규 가입 — userId={}", user.getId());
        return user;
    }

    private AuthResultDto buildResult(UserDto user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.issue(user.getId());
        boolean needsConsent = userMapper.countActiveConsents(user.getId()) == 0;

        return AuthResultDto.builder()
                .response(LoginResponseDto.builder()
                        .accessToken(accessToken)
                        .user(UserMeDto.builder()
                                .id(user.getId())
                                .nickname(user.getNickname())
                                .email(user.getEmail())
                                .build())
                        .needsConsent(needsConsent)
                        .build())
                .refreshToken(refreshToken)
                .build();
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*AuthServiceTest*"`
Expected: PASS (6개 테스트)

- [ ] **Step 7: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/user/ \
        apps/api/src/main/resources/mapper/user/UserMapper.xml \
        apps/api/src/test/java/com/kb/tangtang/user/service/AuthServiceTest.java
git commit -m "feat: 구글 로그인·가입 분기와 토큰 발급 AuthService 구현"
```

---

## Task 6: 인증 인터셉터 · @LoginUser

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/common/auth/LoginUser.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/common/auth/JwtAuthInterceptor.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/common/auth/LoginUserArgumentResolver.java`
- Modify: `apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java`
- Test: `apps/api/src/test/java/com/kb/tangtang/common/auth/JwtAuthInterceptorTest.java`

**Interfaces:**
- Consumes: `JwtProvider` (Task 1)
- Produces:
  - `@LoginUser` — 파라미터 애너테이션
  - `JwtAuthInterceptor.LOGIN_USER_ID` — request attribute 키 상수 `"loginUserId"`
  - `JwtAuthInterceptor(JwtProvider provider, ObjectMapper objectMapper)` — `preHandle` 이 통과 시 `true`, 실패 시 401 JSON 을 쓰고 `false`
  - `LoginUserArgumentResolver` — `@LoginUser Long userId` 주입

- [ ] **Step 1: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/common/auth/JwtAuthInterceptorTest.java`

```java
package com.kb.tangtang.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthInterceptorTest {

    private static final String SECRET = "tangtang-test-secret-key-must-be-32-bytes-or-longer";

    private JwtProvider jwtProvider;
    private JwtAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 900);
        interceptor = new JwtAuthInterceptor(jwtProvider, new ObjectMapper());
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 통과시키고 userId 를 request 에 넣는다")
    void validToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(42L, request.getAttribute(JwtAuthInterceptor.LOGIN_USER_ID));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401 과 UNAUTHORIZED 를 준다")
    void missingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Bearer 접두가 없으면 401 을 준다")
    void malformedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Token abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("만료된 토큰은 401 과 TOKEN_EXPIRED 를 준다")
    void expiredToken() throws Exception {
        String expired = new JwtProvider(SECRET, -60).createAccessToken(42L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + expired);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("CORS preflight(OPTIONS)는 검사하지 않고 통과시킨다")
    void preflightPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("401 응답은 UTF-8 JSON 이다")
    void errorResponseEncoding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*JwtAuthInterceptorTest*"`
Expected: 컴파일 실패 — `JwtAuthInterceptor` 심볼을 찾을 수 없음

- [ ] **Step 3: @LoginUser 작성**

`apps/api/src/main/java/com/kb/tangtang/common/auth/LoginUser.java`

```java
package com.kb.tangtang.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 로그인 사용자 ID 를 주입한다.
 *
 *   @GetMapping("/me")
 *   public ApiResponse<UserMeDto> me(@LoginUser Long userId) { ... }
 *
 * JwtAuthInterceptor 를 통과한 요청에서만 값이 채워진다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
```

- [ ] **Step 4: JwtAuthInterceptor 작성**

`apps/api/src/main/java/com/kb/tangtang/common/auth/JwtAuthInterceptor.java`

```java
package com.kb.tangtang.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authorization: Bearer <jwt> 검증.
 *
 * 인증 실패는 401 이라 CommonExceptionAdvice(BusinessException → 400)를 태우지 않고
 * 여기서 직접 ApiResponse 형태의 JSON 을 쓴다. 응답 포맷은 다른 실패 응답과 동일하다.
 *
 * 적용 범위는 ServletConfig 에서 지정한다 (/api/** 중 /api/health · /api/auth/** 제외).
 */
@Component
@Log4j2
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 통과한 요청의 request attribute 키. LoginUserArgumentResolver 가 읽는다. */
    public static final String LOGIN_USER_ID = "loginUserId";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // CORS preflight 에는 Authorization 헤더가 실리지 않는다. 막으면 본 요청이 아예 못 온다.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            writeError(response, "UNAUTHORIZED", "로그인이 필요합니다.");
            return false;
        }

        try {
            Long userId = jwtProvider.parseUserId(header.substring(PREFIX.length()));
            request.setAttribute(LOGIN_USER_ID, userId);
            return true;
        } catch (BusinessException ex) {
            writeError(response, ex.getCode(), ex.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
```

- [ ] **Step 5: LoginUserArgumentResolver 작성**

`apps/api/src/main/java/com/kb/tangtang/common/auth/LoginUserArgumentResolver.java`

```java
package com.kb.tangtang.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginUser Long userId 파라미터에 JwtAuthInterceptor 가 넣어둔 값을 주입한다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return webRequest.getAttribute(JwtAuthInterceptor.LOGIN_USER_ID, RequestAttributes.SCOPE_REQUEST);
    }
}
```

- [ ] **Step 6: ServletConfig 에 등록**

`apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java` 를 수정한다.

import 추가:
```java
import com.kb.tangtang.common.auth.JwtAuthInterceptor;
import com.kb.tangtang.common.auth.LoginUserArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;
```

클래스 본문 맨 위(`addCorsMappings` 앞)에 필드와 메서드를 추가한다.

```java
    /*
     * 두 컨텍스트 구조 주의:
     *   RootConfig  가 @Controller·@ControllerAdvice 를 제외한 모든 @Component 를 스캔한다.
     *   → JwtAuthInterceptor · LoginUserArgumentResolver 는 루트 컨텍스트의 빈이다.
     *   ServletConfig(자식)는 부모 컨텍스트의 빈을 주입받을 수 있으므로 아래가 동작한다.
     */
    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                // 로그인 자체 경로와 헬스체크는 인증 없이 열어둔다
                .excludePathPatterns("/api/health", "/api/auth/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*JwtAuthInterceptorTest*"`
Expected: PASS (6개 테스트)

- [ ] **Step 8: 전체 빌드 확인**

Run: `./gradlew :apps:api:build`
Expected: BUILD SUCCESSFUL, `apps/api/build/libs/*.war` 생성

- [ ] **Step 9: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/common/auth/ \
        apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java \
        apps/api/src/test/java/com/kb/tangtang/common/auth/JwtAuthInterceptorTest.java
git commit -m "feat: JWT 인증 인터셉터와 @LoginUser 파라미터 주입 추가"
```

---

## Task 7: 인증 엔드포인트 · 쿠키

**Files:**
- Create: `apps/api/src/main/java/com/kb/tangtang/common/auth/AuthCookieWriter.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/controller/AuthController.java`
- Create: `apps/api/src/main/java/com/kb/tangtang/user/controller/UserController.java`
- Test: `apps/api/src/test/java/com/kb/tangtang/user/controller/AuthControllerTest.java`

**Interfaces:**
- Consumes: `AuthService` (Task 5), `UserMapper` (Task 2·5), `GoogleOAuthClient` (Task 4), `@LoginUser` (Task 6)
- Produces: 엔드포인트 5개 (설계서 §5.1)

> **왜 `javax.servlet.http.Cookie` 를 쓰지 않는가**: 서블릿 4.0 의 `Cookie` 에는 `SameSite` 세터가 없다.
> `Cookie` 로 심고 `SameSite` 만 헤더로 덧붙이면 `Set-Cookie` 헤더가 두 개 나가 브라우저마다 다르게 해석된다.
> 그래서 헤더 문자열을 처음부터 직접 만든다.

> **`OAUTH_STATE_MISMATCH` 는 예외로 던지지 않는다.** 설계서 §7 에 코드가 적혀 있지만,
> 콜백은 브라우저 전체 이동이라 JSON 을 반환할 수 없다. state 불일치는 `BusinessException` 대신
> `/login?error=invalid` 리다이렉트로 처리하고 서버 로그에만 남긴다.
> 따라서 이 코드는 **API 응답으로 나타나지 않는다** — `docs/API_SPEC.md` 의 에러 코드 표에도 넣지 않는다.

- [ ] **Step 1: 쿠키 작성 유틸 구현**

`apps/api/src/main/java/com/kb/tangtang/common/auth/AuthCookieWriter.java`

```java
package com.kb.tangtang.common.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

/**
 * 인증 쿠키(리프레시 토큰 · OAuth state) 읽기/쓰기.
 *
 * javax.servlet 4.0 의 Cookie 에는 SameSite 세터가 없어 Set-Cookie 헤더를 직접 만든다.
 * SameSite=None 은 Secure 없이는 브라우저가 쿠키를 버리므로 설정 조합을 검증한다.
 */
@Component
public class AuthCookieWriter {

    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_STATE = "oauth_state";

    /** 리프레시 쿠키는 인증 경로에서만 필요하다. 다른 API 요청에 실려 나가지 않게 좁힌다. */
    private static final String PATH = "/api/auth";
    private static final int STATE_MAX_AGE_SECONDS = 300;

    private final String sameSite;
    private final boolean secure;
    private final long refreshTokenValiditySeconds;

    public AuthCookieWriter(@Value("${auth.cookie.same-site}") String sameSite,
                            @Value("${auth.cookie.secure}") boolean secure,
                            @Value("${jwt.refresh-token-validity}") long refreshTokenValiditySeconds) {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                    "auth.cookie.same-site=None 은 auth.cookie.secure=true 가 있어야 한다. "
                            + "Secure 없는 SameSite=None 쿠키는 브라우저가 버린다.");
        }
        this.sameSite = sameSite;
        this.secure = secure;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public void writeRefreshToken(HttpServletResponse response, String rawToken) {
        response.addHeader("Set-Cookie",
                build(REFRESH_TOKEN, rawToken, (int) refreshTokenValiditySeconds));
    }

    public void clearRefreshToken(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build(REFRESH_TOKEN, "", 0));
    }

    public void writeState(HttpServletResponse response, String state) {
        response.addHeader("Set-Cookie", build(OAUTH_STATE, state, STATE_MAX_AGE_SECONDS));
    }

    public void clearState(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build(OAUTH_STATE, "", 0));
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(javax.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private String build(String name, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder()
                .append(name).append('=').append(value)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; Path=").append(PATH)
                .append("; HttpOnly")
                .append("; SameSite=").append(sameSite);
        if (secure) {
            sb.append("; Secure");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`apps/api/src/test/java/com/kb/tangtang/user/controller/AuthControllerTest.java`

```java
package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.AuthCookieWriter;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.service.AuthService;
import com.kb.tangtang.user.service.GoogleOAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String FRONT_URL = "http://localhost:5173";

    @Mock private AuthService authService;
    @Mock private GoogleOAuthClient googleOAuthClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthCookieWriter cookieWriter = new AuthCookieWriter("Lax", false, 1209600);
        AuthController controller = new AuthController(authService, googleOAuthClient, cookieWriter, FRONT_URL);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private static AuthResultDto authResult() {
        return AuthResultDto.builder()
                .response(LoginResponseDto.builder()
                        .accessToken("access-jwt")
                        .user(UserMeDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com").build())
                        .needsConsent(true)
                        .build())
                .refreshToken("refresh-raw")
                .build();
    }

    @Test
    @DisplayName("GET /api/auth/google 은 구글로 302 하고 state 쿠키를 심는다")
    void googleRedirect() throws Exception {
        when(googleOAuthClient.buildAuthorizationUrl(anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=x");

        MvcResult result = mockMvc.perform(get("/api/auth/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals("https://accounts.google.com/o/oauth2/v2/auth?state=x",
                result.getResponse().getRedirectedUrl());
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("oauth_state="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("콜백이 성공하면 리프레시 쿠키를 심고 프론트 /auth/callback 으로 보낸다")
    void callbackSuccess() throws Exception {
        when(authService.loginWithGoogleCode("the-code")).thenReturn(authResult());

        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/auth/callback", result.getResponse().getRedirectedUrl());
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                        .anyMatch(header -> header.startsWith("refresh_token=refresh-raw")),
                "리프레시 토큰이 쿠키로 나가야 한다");
        assertTrue(result.getResponse().getRedirectedUrl().indexOf("refresh-raw") < 0,
                "토큰이 URL 에 노출되면 안 된다");
    }

    @Test
    @DisplayName("state 가 쿠키와 다르면 로그인 화면으로 error=invalid 를 붙여 되돌린다")
    void callbackStateMismatch() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "attacker-state")
                        .cookie(new Cookie("oauth_state", "real-state")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=invalid", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("state 쿠키가 아예 없어도 로그인 화면으로 되돌린다")
    void callbackMissingStateCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=invalid", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("사용자가 구글에서 취소하면 error=cancelled 로 되돌린다")
    void callbackUserCancelled() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("error", "access_denied")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=cancelled", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("로그인 처리 중 업무 예외가 나면 코드에 맞는 쿼리로 되돌린다")
    void callbackBusinessError() throws Exception {
        when(authService.loginWithGoogleCode("the-code"))
                .thenThrow(new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다."));

        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "the-code")
                        .param("state", "st-1")
                        .cookie(new Cookie("oauth_state", "st-1")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertEquals(FRONT_URL + "/login?error=withdrawn", result.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("POST /api/auth/refresh 는 새 액세스 토큰과 사용자 정보를 준다")
    void refresh() throws Exception {
        when(authService.refresh("old-raw")).thenReturn(authResult());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "old-raw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.user.nickname").value("지윤"))
                .andExpect(jsonPath("$.data.needsConsent").value(true));
    }

    @Test
    @DisplayName("쿠키 없이 refresh 하면 400 과 INVALID_TOKEN 이다")
    void refreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃은 토큰을 폐기하고 쿠키를 만료시킨다")
    void logout() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", "raw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        verify(authService).logout("raw");
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                        .anyMatch(header -> header.startsWith("refresh_token=") && header.contains("Max-Age=0")),
                "쿠키를 만료시켜야 한다");
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인**

Run: `./gradlew :apps:api:test --tests "*AuthControllerTest*"`
Expected: 컴파일 실패 — `AuthController` 심볼을 찾을 수 없음

- [ ] **Step 4: AuthController 구현**

`apps/api/src/main/java/com/kb/tangtang/user/controller/AuthController.java`

```java
package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.AuthCookieWriter;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.service.AuthService;
import com.kb.tangtang.user.service.GoogleOAuthClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 구글 OAuth 로그인 엔드포인트.
 *
 * /google 과 /google/callback 은 브라우저 전체 이동(302)이라 JSON 을 반환하지 않는다.
 * 콜백에서 예외가 나면 JSON 을 뿌리는 대신 프론트 로그인 화면으로 error 쿼리를 달아 되돌린다.
 * (브라우저 주소창에 JSON 이 뜨는 상황을 막는다)
 */
@RestController
@RequestMapping("/api/auth")
@Log4j2
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;
    private final GoogleOAuthClient googleOAuthClient;
    private final AuthCookieWriter cookieWriter;
    private final String frontUrl;

    public AuthController(AuthService authService,
                          GoogleOAuthClient googleOAuthClient,
                          AuthCookieWriter cookieWriter,
                          @Value("${app.front-url}") String frontUrl) {
        this.authService = authService;
        this.googleOAuthClient = googleOAuthClient;
        this.cookieWriter = cookieWriter;
        this.frontUrl = frontUrl;
    }

    /** 구글 동의 화면으로 보낸다. state 를 만들어 쿠키에도 남긴다(CSRF 방지). */
    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String state = randomState();
        cookieWriter.writeState(response, state);
        response.sendRedirect(googleOAuthClient.buildAuthorizationUrl(state));
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {

        String savedState = cookieWriter.read(request, AuthCookieWriter.OAUTH_STATE).orElse(null);
        cookieWriter.clearState(response);   // state 는 1회용이다

        if (error != null) {
            // 사용자가 구글 동의 화면에서 취소한 경우가 대부분이다
            log.info("구글 인증 취소/실패 — {}", error);
            response.sendRedirect(frontUrl + "/login?error=cancelled");
            return;
        }

        if (code == null || state == null || savedState == null || !savedState.equals(state)) {
            log.warn("OAuth state 불일치 — CSRF 의심");
            response.sendRedirect(frontUrl + "/login?error=invalid");
            return;
        }

        try {
            AuthResultDto result = authService.loginWithGoogleCode(code);
            cookieWriter.writeRefreshToken(response, result.getRefreshToken());
            response.sendRedirect(frontUrl + "/auth/callback");
        } catch (BusinessException ex) {
            log.warn("로그인 실패 [{}] {}", ex.getCode(), ex.getMessage());
            response.sendRedirect(frontUrl + "/login?error=" + toFrontErrorCode(ex.getCode()));
        }
    }

    /** 액세스 토큰 재발급. 리프레시 토큰은 회전하며 새 쿠키로 덮어쓴다. */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = cookieWriter.read(request, AuthCookieWriter.REFRESH_TOKEN).orElse(null);

        AuthResultDto result = authService.refresh(rawToken);
        cookieWriter.writeRefreshToken(response, result.getRefreshToken());
        return ApiResponse.ok(result.getResponse());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieWriter.read(request, AuthCookieWriter.REFRESH_TOKEN).ifPresent(authService::logout);
        cookieWriter.clearRefreshToken(response);
        return ApiResponse.ok();
    }

    private static String randomState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 백엔드 에러 코드를 프론트 쿼리스트링용 짧은 값으로 바꾼다. */
    private static String toFrontErrorCode(String code) {
        switch (code) {
            case "USER_WITHDRAWN":
                return "withdrawn";
            case "OAUTH_TOKEN_EXCHANGE_FAILED":
                return "failed";
            default:
                return "failed";
        }
    }
}
```

- [ ] **Step 5: UserController 구현**

`apps/api/src/main/java/com/kb/tangtang/user/controller/UserController.java`

```java
package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자 본인 정보.
 * JwtAuthInterceptor 를 통과한 요청만 도달하므로 userId 는 항상 채워져 있다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeDto> me(@LoginUser Long userId) {
        UserDto user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(UserMeDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build());
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :apps:api:test --tests "*AuthControllerTest*"`
Expected: PASS (9개 테스트)

- [ ] **Step 7: 백엔드 전체 테스트**

Run: `./gradlew :apps:api:test`
Expected: PASS. `UserMapperTest`·`DataSourceConnectionTest` 는 skipped

- [ ] **Step 8: 커밋**

```bash
git add apps/api/src/main/java/com/kb/tangtang/common/auth/AuthCookieWriter.java \
        apps/api/src/main/java/com/kb/tangtang/user/controller/ \
        apps/api/src/test/java/com/kb/tangtang/user/controller/
git commit -m "feat: 구글 로그인 엔드포인트와 리프레시 쿠키 처리 추가"
```

---

## Task 8: 프론트 인증 기반 — 스토어 · API · HTTP 인터셉터

**Files:**
- Create: `apps/web/src/stores/auth.js`
- Create: `apps/web/src/api/auth.js`
- Modify: `apps/web/src/api/http.js`

**Interfaces:**
- Consumes: 백엔드 `POST /api/auth/refresh` 응답 `{ accessToken, user: { id, nickname, email }, needsConsent }`
- Produces:
  - `useAuthStore()` — `accessToken`, `user`, `needsConsent`, `isLoggedIn`(computed), `setSession(payload)`, `clear()`
  - `api/auth.js` — `refreshSession()`, `logout()`, `fetchMe()`, `GOOGLE_LOGIN_URL`
  - `http.js` 기본 export 는 그대로 axios 인스턴스

> **순환 import 주의**: `http.js`가 `api/auth.js`를 import 하면 `auth.js → http.js → auth.js` 순환이 생긴다.
> 재발급 호출은 `http.js` 안에 **인터셉터 없는 별도 axios 인스턴스**를 만들어 처리한다.

- [ ] **Step 1: 인증 스토어 작성**

`apps/web/src/stores/auth.js`

```js
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

/**
 * 인증 상태는 여기 한 곳에서만 관리한다. (apps/web/AGENTS.md 규칙)
 *
 * accessToken 은 메모리에만 둔다. localStorage 에 넣지 않는다 —
 * XSS 한 번에 토큰이 통째로 털리기 때문이다.
 * 새로고침하면 사라지지만, main.js 부팅 시퀀스가 httpOnly 쿠키로
 * 재발급받아 복원한다.
 */
export const useAuthStore = defineStore('auth', () => {
    const accessToken = ref('');
    const user = ref(null);
    /**
     * 필수 동의를 아직 하지 않은 사용자인지.
     * 동의 화면은 후속 이슈라 지금은 저장만 하고 라우팅에 쓰지 않는다.
     */
    const needsConsent = ref(false);

    const isLoggedIn = computed(() => Boolean(accessToken.value));

    function setSession(session) {
        accessToken.value = session.accessToken ?? '';
        user.value = session.user ?? null;
        needsConsent.value = Boolean(session.needsConsent);
    }

    function clear() {
        accessToken.value = '';
        user.value = null;
        needsConsent.value = false;
    }

    return { accessToken, user, needsConsent, isLoggedIn, setSession, clear };
});
```

- [ ] **Step 2: 인증 API 모듈 작성**

`apps/web/src/api/auth.js`

```js
import http from './http';

/**
 * 구글 로그인 시작 주소.
 * SPA 라우팅이 아니라 window.location.href 로 전체 이동해야 한다 —
 * 백엔드가 302 로 구글에 넘기는 구조이기 때문이다.
 * http 인스턴스의 baseURL('/api')을 타지 않으므로 전체 경로를 적는다.
 */
export const GOOGLE_LOGIN_URL = '/api/auth/google';

/** 리프레시 쿠키로 액세스 토큰을 재발급한다. { accessToken, user, needsConsent } */
export function refreshSession() {
    return http.post('/auth/refresh');
}

export function logout() {
    return http.post('/auth/logout');
}

/** 로그인한 본인 정보. { id, nickname, email } */
export function fetchMe() {
    return http.get('/users/me');
}
```

- [ ] **Step 3: http.js 수정**

`apps/web/src/api/http.js` 전체를 아래로 교체한다.

```js
import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

/**
 * 프로젝트 공용 axios 인스턴스.
 * 모든 API 호출은 이 인스턴스를 통해서만 한다. (컴포넌트에서 axios 직접 import 금지)
 *
 * 백엔드는 공통 래퍼로 응답한다:
 *   성공 { success: true,  data: ... }
 *   실패 { success: false, code: "...", message: "..." }
 * 인터셉터에서 data 만 꺼내주므로, 호출부는 실제 payload 만 받는다.
 *
 * withCredentials: 리프레시 토큰이 httpOnly 쿠키라 자격증명을 같이 보내야 한다.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const http = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
});

/*
 * 재발급 전용 인스턴스.
 * 아래 응답 인터셉터가 달려 있지 않아야 401 → refresh → 401 무한 재귀가 생기지 않는다.
 * api/auth.js 를 import 하면 http.js ↔ auth.js 순환이 생기므로 여기서 직접 만든다.
 */
const refreshClient = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
});

/*
 * 동시에 여러 요청이 401 을 받으면 재발급이 N 번 날아간다.
 * 리프레시 토큰은 회전 방식이라 두 번째 호출부터는 "폐기된 토큰 재사용"으로 감지돼
 * 사용자의 전체 토큰이 폐기된다. 그래서 진행 중인 재발급을 하나로 묶는다.
 */
let refreshPromise = null;

function requestRefresh() {
    if (!refreshPromise) {
        refreshPromise = refreshClient
            .post('/auth/refresh')
            .then((response) => response.data.data)
            .finally(() => {
                refreshPromise = null;
            });
    }
    return refreshPromise;
}

// 요청: 액세스 토큰 주입
http.interceptors.request.use((config) => {
    const { accessToken } = useAuthStore();
    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
});

// 응답: 공통 래퍼 언랩 + 401 재발급 재시도 + 에러 정규화
http.interceptors.response.use(
    (response) => {
        const body = response.data;
        if (body && typeof body.success === 'boolean') {
            if (!body.success) {
                return Promise.reject(new ApiError(body.code, body.message, response.status));
            }
            return body.data;
        }
        return body;
    },
    async (error) => {
        const config = error.config;
        const status = error.response?.status;
        const isAuthCall = config?.url?.startsWith('/auth/');

        // 재시도는 요청당 1회. 인증 경로 자체는 재시도하지 않는다.
        if (status === 401 && config && !config.isRetry && !isAuthCall) {
            config.isRetry = true;
            try {
                const session = await requestRefresh();
                useAuthStore().setSession(session);
                config.headers.Authorization = `Bearer ${session.accessToken}`;
                return http(config);
            } catch {
                // 재발급도 실패 = 세션이 끝났다. 로그인 화면으로 되돌린다.
                useAuthStore().clear();
                window.location.assign('/login?error=expired');
            }
        }

        const body = error.response?.data;
        return Promise.reject(
            new ApiError(
                body?.code ?? 'NETWORK_ERROR',
                body?.message ?? '서버와 통신할 수 없습니다.',
                error.response?.status ?? 0,
            ),
        );
    },
);

export class ApiError extends Error {
    constructor(code, message, status) {
        super(message);
        this.name = 'ApiError';
        this.code = code;
        this.status = status;
    }
}

export default http;
```

- [ ] **Step 4: 린트·포맷 확인**

Run: `cd apps/web && npm run lint:check && npm run format:check`
Expected: 통과. 실패하면 `npm run lint && npm run format` 으로 자동 수정한 뒤 다시 확인한다.

- [ ] **Step 5: 커밋**

```bash
git add apps/web/src/stores/auth.js apps/web/src/api/auth.js apps/web/src/api/http.js
git commit -m "feat: 프론트 인증 스토어와 토큰 자동 재발급 처리 추가"
```

---

## Task 9: 로그인 화면

**Files:**
- Create: `apps/web/src/assets/images/google-logo.svg`
- Create: `apps/web/src/components/auth/GoogleSignInButton.vue`
- Create: `apps/web/src/views/auth/LoginView.vue`

**Interfaces:**
- Consumes: `GOOGLE_LOGIN_URL` (Task 8)
- Produces: `LoginView.vue` (라우트 `/login` 에서 사용), `GoogleSignInButton.vue` (`@click-login` 이벤트)

> **디자인 원본**: `doc/개발산출물/화면설계/figma_tangtang/home/download 8.png`
> 상단 라벨은 Figma 의 `CPR · CASH POCKET RESCUE` 대신 **`탕탕 · 지갑재판소`** 로 넣는다(구 서비스명).

- [ ] **Step 1: 구글 로고 자산 추가**

`apps/web/src/assets/images/google-logo.svg`

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48" role="img" aria-label="Google">
    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
</svg>
```

- [ ] **Step 2: 구글 버튼 컴포넌트 작성**

`apps/web/src/components/auth/GoogleSignInButton.vue`

```vue
<!--
  용도: 구글 로그인 시작 버튼. 로그인 화면 전용이다.
  언제 쓰는지: /login 화면 하나. 세 번째 화면에서 또 필요해지면 common/ 으로 올린다(3의 법칙).
  쓰면 안 되는 경우: 일반 액션 버튼 — BaseButton 을 쓴다.

  BaseButton 을 쓰지 않는 이유: Figma 의 남색 배경이 BaseButton 의 4개 variant 에 없고,
  구글 로고 슬롯까지 넣으면 범용 버튼이 오염된다.
-->
<script setup>
defineProps({
    label: { type: String, default: 'Google로 판결 시작하기' },
});

const emit = defineEmits(['click-login']);
</script>

<template>
    <button class="google-signin" type="button" @click="emit('click-login')">
        <span class="google-signin__logo">
            <img src="@/assets/images/google-logo.svg" alt="" width="20" height="20" />
        </span>
        <span class="google-signin__label">{{ label }}</span>
    </button>
</template>

<style scoped>
.google-signin {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-3);
    width: 100%;
    height: 60px;
    padding: 0 var(--tt-space-5);
    border: none;
    border-radius: var(--tt-radius-lg);
    background: var(--tt-text);
    color: var(--tt-text-inverse);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: 1;
    cursor: pointer;
    transition: filter 0.15s ease;
}

.google-signin:hover {
    filter: brightness(1.15);
}

/* 구글 브랜드 가이드라인 — 로고는 흰 배경 위에 둔다 */
.google-signin__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
}
</style>
```

- [ ] **Step 3: 로그인 화면 작성**

`apps/web/src/views/auth/LoginView.vue`

```vue
<!--
  용도: 구글 로그인 진입 화면. figma_tangtang/home/download 8.png 기준.
  언제 쓰는지: 라우트 /login. 미로그인 사용자가 보호된 화면에 접근하면 가드가 여기로 보낸다.
  쓰면 안 되는 경우: 로그인 후 화면 — 탭바가 있는 레이아웃을 쓴다.
-->
<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { GOOGLE_LOGIN_URL } from '@/api/auth';
import GoogleSignInButton from '@/components/auth/GoogleSignInButton.vue';

const route = useRoute();

const ERROR_MESSAGES = {
    invalid: '로그인 요청이 올바르지 않습니다. 다시 시도해 주세요.',
    failed: '구글 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    withdrawn: '이용할 수 없는 계정입니다.',
    security: '보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.',
    expired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
    cancelled: '',
};

const errorMessage = computed(() => ERROR_MESSAGES[route.query.error] ?? '');

/*
 * SPA 라우팅이 아니라 전체 이동이다.
 * 백엔드가 state 쿠키를 심고 구글로 302 하는 구조라 router.push 로는 동작하지 않는다.
 */
function startGoogleLogin() {
    window.location.href = GOOGLE_LOGIN_URL;
}
</script>

<template>
    <div class="login">
        <p class="login__brand">탕탕 · 지갑재판소</p>

        <div class="login__hero">
            <img class="login__mascot" src="@/assets/images/tangtang.png" alt="" />
        </div>

        <p class="login__case">오늘의 사건 No. 001</p>

        <h1 class="login__title">오늘의 소비,<br />판결을 시작합니다</h1>

        <p class="login__lead">소비 기록을 증거로 확인하고<br />더 나은 금융 습관을 만들어보세요.</p>

        <div class="login__status">
            <span class="login__status-label">소비 습관 개선 사건</span>
            <span class="login__status-value">판결 준비 완료</span>
        </div>

        <p v-if="errorMessage" class="login__error" role="alert">{{ errorMessage }}</p>

        <div class="login__action">
            <GoogleSignInButton @click-login="startGoogleLogin" />
            <p class="login__terms">이용약관 · 개인정보처리방침</p>
        </div>
    </div>
</template>

<style scoped>
.login {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 100vh;
    padding: var(--tt-space-8) var(--tt-space-6) var(--tt-space-10);
    background: var(--tt-bg-subtle);
    text-align: center;
}

.login__brand {
    margin-bottom: var(--tt-space-6);
    color: var(--tt-primary);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.08em;
}

.login__hero {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 180px;
    height: 180px;
    margin-bottom: var(--tt-space-5);
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary-subtle);
}

.login__mascot {
    width: 130px;
    height: auto;
}

.login__case {
    margin-bottom: var(--tt-space-5);
    padding: var(--tt-space-2) var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    color: var(--tt-text);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
}

.login__title {
    margin-bottom: var(--tt-space-4);
    color: var(--tt-text);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.login__lead {
    margin-bottom: var(--tt-space-8);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
}

.login__status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-4);
    width: 100%;
    padding: var(--tt-space-5);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

.login__status-label {
    color: var(--tt-text);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.login__status-value {
    color: var(--tt-primary);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.login__error {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3) var(--tt-space-4);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
}

.login__action {
    width: 100%;
    margin-top: auto;
    padding-top: var(--tt-space-10);
}

.login__terms {
    margin-top: var(--tt-space-4);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
}
</style>
```

- [ ] **Step 4: 린트·포맷 확인**

Run: `cd apps/web && npm run lint:check && npm run format:check`
Expected: 통과

- [ ] **Step 5: 커밋**

```bash
git add apps/web/src/assets/images/google-logo.svg \
        apps/web/src/components/auth/GoogleSignInButton.vue \
        apps/web/src/views/auth/LoginView.vue
git commit -m "feat: 구글 로그인 화면과 버튼 컴포넌트 구현"
```

---

## Task 10: 라우팅 연결 · 세션 복원 · 통합 검증

**Files:**
- Create: `apps/web/src/views/auth/AuthCallbackView.vue`
- Modify: `apps/web/src/router/index.js`
- Modify: `apps/web/src/App.vue`
- Modify: `apps/web/src/main.js`
- Modify: `apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java`
- Create: `docs/API_SPEC.md`

**Interfaces:**
- Consumes: `useAuthStore` · `refreshSession` (Task 8), `LoginView` (Task 9), 백엔드 엔드포인트 5개 (Task 7)
- Produces: 동작하는 로그인 플로우 전체

- [ ] **Step 1: 콜백 착지 화면 작성**

`apps/web/src/views/auth/AuthCallbackView.vue`

```vue
<!--
  용도: 구글 로그인 후 백엔드가 되돌려보내는 착지 지점.
  언제 쓰는지: 라우트 /auth/callback. 사용자가 직접 열 일은 없다.
  쓰면 안 되는 경우: 여기서 refresh 를 다시 호출하는 것.

  refresh 를 부르지 않는 이유:
  OAuth 리다이렉트는 전체 페이지 이동이라 앱이 새로 부팅되고,
  main.js 부팅 시퀀스가 이미 refresh 를 1회 수행한다.
  여기서 또 부르면 리프레시 토큰이 회전 방식이라 두 번째 호출이
  "폐기된 토큰 재사용" 으로 감지돼 전체 토큰이 폐기된다.
-->
<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import StateLoading from '@/components/common/StateLoading.vue';

const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
    if (auth.isLoggedIn) {
        router.replace({ name: 'home' });
    } else {
        router.replace({ name: 'login', query: { error: 'failed' } });
    }
});
</script>

<template>
    <div class="auth-callback">
        <StateLoading message="로그인 중" />
    </div>
</template>

<style scoped>
.auth-callback {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--tt-bg-subtle);
}
</style>
```

- [ ] **Step 2: 라우터에 라우트 2개와 가드 추가**

`apps/web/src/router/index.js` 전체를 아래로 교체한다.

```js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

/*
 * 하단 5탭 구조: 재판 · 자산 · 홈 · 장부 · 마이 (TheTabBar.vue 의 TABS 와 짝을 이룬다).
 * 각 화면 담당자는 아래 component 한 줄을 자기 뷰로 바꾸면 된다.
 *   component: () => import('@/views/trial/TrialHomeView.vue')
 *
 * meta.public   — 로그인 없이 접근 가능
 * meta.hideTabBar — 하단 탭바를 숨긴다 (App.vue 가 읽는다)
 */
const routes = [
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '로그인', public: true, hideTabBar: true },
    },
    {
        path: '/auth/callback',
        name: 'authCallback',
        component: () => import('@/views/auth/AuthCallbackView.vue'),
        meta: { title: '로그인 처리 중', public: true, hideTabBar: true },
    },
    {
        path: '/',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '홈' },
    },
    {
        path: '/trial',
        name: 'trial',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '재판' },
    },
    {
        path: '/asset',
        name: 'asset',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '자산' },
    },
    {
        path: '/ledger',
        name: 'ledger',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '장부' },
    },
    {
        path: '/my',
        name: 'my',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '마이' },
    },
];

/* 개발용 컴포넌트 카탈로그. import.meta.env.DEV 가 false 인 프로덕션 빌드에서는
 * 이 블록째로 제거돼 라우트도 청크도 생기지 않는다.
 * 사용자 데이터를 다루지 않으므로 로그인 없이 연다. */
if (import.meta.env.DEV) {
    routes.push({
        path: '/dev/ui',
        name: 'devUi',
        component: () => import('@/views/dev/UiCatalogView.vue'),
        meta: { title: '컴포넌트 카탈로그', public: true, hideTabBar: true },
    });
}

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

/*
 * 로그인 가드. meta.public 이 아닌 모든 화면은 로그인이 필요하다.
 * 5탭은 전부 개인 금융 데이터라 예외를 두지 않는다.
 * 개발용 인증 우회 플래그는 만들지 않는다 — 인증 버그를 가리고 운영 설정에 새어나간다.
 */
router.beforeEach((to) => {
    const auth = useAuthStore();

    if (to.meta.public) {
        // 이미 로그인한 사용자가 로그인 화면으로 오면 홈으로 보낸다
        if (auth.isLoggedIn && to.name === 'login') {
            return { name: 'home' };
        }
        return true;
    }

    if (!auth.isLoggedIn) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }

    return true;
});

export default router;
```

- [ ] **Step 3: App.vue 에 탭바 조건부 렌더 적용**

`apps/web/src/App.vue` 전체를 아래로 교체한다.

```vue
<script setup>
import { useRoute } from 'vue-router';
import TheTabBar from '@/components/common/TheTabBar.vue';

const route = useRoute();
</script>

<template>
    <div class="tt-app">
        <main class="tt-app__content" :class="{ 'tt-app__content--bare': route.meta.hideTabBar }">
            <RouterView />
        </main>
        <TheTabBar v-if="!route.meta.hideTabBar" />
    </div>
</template>

<style scoped>
.tt-app {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: var(--tt-content-max);
    min-height: 100vh;
    margin: 0 auto;
    background: var(--tt-bg);
}

/* 하단 탭바에 가리지 않도록 콘텐츠 아래 여백을 확보한다 */
.tt-app__content {
    flex: 1;
    padding-bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-4));
}

/* 탭바가 없는 화면(로그인 등)은 여백도 필요 없다 */
.tt-app__content--bare {
    padding-bottom: 0;
}
</style>
```

- [ ] **Step 4: main.js 에 세션 복원 추가**

`apps/web/src/main.js` 전체를 아래로 교체한다.

```js
import './assets/tokens.css';
import './assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import { refreshSession } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

/*
 * 부팅 시 세션 복원.
 * 액세스 토큰은 메모리에만 있어 새로고침하면 사라진다. httpOnly 리프레시 쿠키로
 * 한 번 재발급받아 로그인 상태를 되살린다. 구글 콜백에서 돌아온 직후에도 이 경로를 탄다.
 *
 * mount 전에 끝내야 라우터 가드가 올바른 로그인 상태를 보고 판단한다.
 * 실패는 정상 흐름(비로그인)이므로 조용히 넘어간다.
 */
try {
    const session = await refreshSession();
    useAuthStore(pinia).setSession(session);
} catch {
    // 비로그인 상태로 진행한다
}

app.mount('#app');
```

- [ ] **Step 5: 백엔드 CORS 에 로컬 오리진 추가**

`apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java` 의 `addCorsMappings` 를 수정한다.

```java
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 로컬 개발은 Vite 프록시(same-origin)라 CORS 를 타지 않지만,
                // 프록시를 끄고 직접 붙이는 경우를 위해 남겨둔다.
                .allowedOrigins("https://monorepo-three-ruby-81.vercel.app", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
```

- [ ] **Step 6: 린트·포맷·빌드 확인**

Run: `cd apps/web && npm run lint:check && npm run format:check && npm run build`
Expected: 전부 통과. `dist/` 생성

Run: `./gradlew :apps:api:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Google Cloud Console 리다이렉트 URI 등록 확인**

Google Cloud Console > API 및 서비스 > 사용자 인증 정보 > OAuth 2.0 클라이언트 ID 에서
**승인된 리디렉션 URI** 에 아래가 정확히 등록돼 있는지 확인한다. 없으면 추가한다.

```
http://localhost:5173/api/auth/google/callback
```

`application.properties` 의 `google.oauth.redirect-uri` 와 **문자 하나까지 같아야** 한다.
다르면 구글이 `redirect_uri_mismatch` 로 거부한다.

- [ ] **Step 8: 로컬 통합 검증 (수동)**

톰캣에 war 를 올리고 프론트 개발 서버를 띄운 뒤 아래를 순서대로 확인한다.

```bash
# 터미널 1 — 백엔드 (IDE 톰캣 실행 또는 war 배포). :8080
# 터미널 2
cd apps/web && npm run dev
```

브라우저에서 확인할 것:

1. `http://localhost:5173/asset` 접속 → **`/login` 으로 리다이렉트**되는가
2. `/login` 에서 "Google로 판결 시작하기" 클릭 → 구글 동의 화면이 뜨는가
3. 계정 선택 후 → **홈(`/`)에 도착**하는가, 하단 탭바가 보이는가
4. DevTools > Application > Cookies → `refresh_token` 이 **HttpOnly 체크됨** 상태인가
5. DevTools > Application > Local Storage → **비어 있는가** (토큰이 없어야 한다)
6. 주소창 URL 에 토큰 문자열이 **없는가**
7. **F5 새로고침** → 로그인이 유지되는가 (Network 탭에 `/api/auth/refresh` 200 이 보임)
8. `mysql -u tangtang -p tangtang -e "SELECT id, nickname, status, difficulty_id FROM tbl_user"` → 행이 생겼고 `difficulty_id=1` 인가
9. `mysql -u tangtang -p tangtang -e "SELECT id, user_id, is_revoked FROM tbl_refresh_token"` → 새로고침할 때마다 행이 늘고 이전 행이 `is_revoked=1` 이 되는가 (회전 확인)
10. 구글 동의 화면에서 **취소** → `/login` 으로 돌아오고 에러 문구가 안 뜨는가

하나라도 실패하면 고치고 이 단계를 다시 돌린다.

- [ ] **Step 9: API 규격 문서 작성**

`docs/API_SPEC.md` (신규 — 루트 `AGENTS.md` 가 참조하는데 파일이 없었다)

```markdown
# API 규격

모든 응답은 공통 래퍼 `com.kb.tangtang.common.dto.ApiResponse` 로 감싼다.

```
성공  { "success": true,  "data": { ... } }
실패  { "success": false, "code": "NOT_FOUND", "message": "..." }
```

인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 보낸다.
리프레시 토큰은 httpOnly 쿠키(`refresh_token`, `Path=/api/auth`)로만 오간다.

## 공통

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/health` | 불필요 | `{ status, service }` |

## 인증 (이슈 #9)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/auth/google` | 불필요 | 302 → 구글 동의 화면. `oauth_state` 쿠키 발급 |
| GET | `/api/auth/google/callback` | 불필요 | 302 → 프론트. 성공 시 `/auth/callback` + `refresh_token` 쿠키, 실패 시 `/login?error=...` |
| POST | `/api/auth/refresh` | 쿠키 | `{ accessToken, user: { id, nickname, email }, needsConsent }` |
| POST | `/api/auth/logout` | 쿠키 | `{}` + 쿠키 만료 |
| GET | `/api/users/me` | Bearer | `{ id, nickname, email }` |

### 인증 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `UNAUTHORIZED` | 401 | Authorization 헤더 없음/형식 오류 |
| `TOKEN_EXPIRED` | 401 | 액세스 토큰 만료 — 프론트가 자동 재발급한다 |
| `INVALID_TOKEN` | 401·400 | 서명 위조·형식 오류·리프레시 토큰 없음/만료 |
| `REFRESH_TOKEN_REUSED` | 400 | 폐기된 리프레시 토큰 재사용 — 전체 토큰 폐기됨 |
| `USER_WITHDRAWN` | 400 | 탈퇴·차단 계정 |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | 400 | 구글 code↔token 교환 실패 |

### 콜백 리다이렉트 error 쿼리

`cancelled`(사용자 취소) · `invalid`(state 불일치) · `failed`(교환 실패) · `withdrawn`(이용 불가 계정) · `expired`(세션 만료)
```

- [ ] **Step 10: 커밋**

```bash
git add apps/web/src/views/auth/AuthCallbackView.vue apps/web/src/router/index.js \
        apps/web/src/App.vue apps/web/src/main.js \
        apps/api/src/main/java/com/kb/tangtang/config/ServletConfig.java \
        docs/API_SPEC.md
git commit -m "feat: 로그인 라우트 가드와 부팅 시 세션 복원 연결"
```

- [ ] **Step 11: PR 준비**

`.claude/skills/pr-check` 절차대로 셀프 점검한 뒤 PR 을 만든다.

```bash
git push -u origin feature/9-auth-google-login
```

PR 제목: `[feat] 구글 OAuth 로그인 구현 및 UI 개발 #9`

PR 본문에 **반드시 포함할 것**:
- `db/AGENTS.md` 규칙 변경 — "로컬 개발 계정은 예외" 폐지. 팀원은 `ALTER USER` 한 줄로 비밀번호 재설정 필요
- `db/schema.sql`·`db/seed.sql` 신설 — 팀원 전원 재세팅 필요
- 팀 공유 파일 수정 — `config/ServletConfig.java`, `api/http.js`, `router/index.js`, `App.vue`, `main.js`
- **로그인 가드가 전면 적용**되어 다른 담당자도 `application-local.properties` 에 구글 client-id/secret 을 채워야 자기 화면에 접근 가능
- 운영 도메인 통합(서드파티 쿠키)은 이번 범위 밖 — 별도 이슈

---

## 후속 이슈로 넘기는 것

| 항목 | 근거 |
|---|---|
| 운영 도메인 통합 (`/api` 프록시) | 설계서 §8.2 — Vercel·API 도메인 분리 시 리프레시 쿠키가 서드파티 쿠키가 된다 |
| 프론트 테스트 러너(Vitest) 도입 | 설계서 §9 — 팀 논의 필요 |
| Figma 로그인 화면의 `CPR` 표기 수정 | 설계서 §6.6 — 디자인 담당 |
| 약관 동의 화면 (`needsConsent` 게이트 연결) | 이슈 분할 — 동의 풀스택 |
| 계좌 연동 3화면 | 이슈 분할 — 계좌연동 풀스택 |
| 마이페이지 로그아웃 UI | API 는 이번에 제공, 화면은 마이페이지 소관 |

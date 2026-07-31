# apps/api — 백엔드 규칙

루트 `AGENTS.md` 의 규칙을 전제로 한다. 여기에는 **이 폴더에서만 다른 것**만 적는다.

## 패키지 구조

`com.kb.tangtang.<모듈>` — 모듈 9개. 패키지 = 논리 모듈, 1:1.

```
common          공통 응답 래퍼 · 공통 예외 · 유틸 (여기에만 둔다)
user            회원 · 인증
account         계좌·카드 연동 (CODEF)
transaction     거래내역 수집·조회
fixedexpense    고정지출 탐지(기소) · 절약 시뮬레이션
mission         데일리 미션
challenge       개인·그룹 챌린지
report          월간 리포트(판결문) · 절감액 검증
notification    알림 · SSE · DLQ
config          앱 전역 설정 (RootConfig / ServletConfig / WebConfig)
```

각 모듈 내부는 `controller / service / mapper / dto / domain` 으로 나눈다.

## 모듈 경계 (중요)

- 모듈 간 **직접 Service 호출을 최소화**한다. 상태 변화 전파는 **Spring Event** 로 한다.
  - 예) 고정지출 탐지 완료 → `FixedExpenseDetectedEvent` 발행 → `report`·`notification` 이 수신
  - 이벤트 클래스명은 `<도메인><과거분사>Event`
- 공용 유틸·응답 래퍼·공통 예외는 **`common` 에만** 둔다. 모듈끼리 복사하지 않는다.
- 새 모듈을 만들 필요는 없다. 위 9개 안에서 해결한다.

## 스프링 설정 (건드리기 전에 읽을 것)

- 스캔이 두 컨텍스트로 나뉘어 있다.
  - `ServletConfig` — `@Controller` · `@ControllerAdvice` 만
  - `RootConfig` — 그 둘을 **제외한** 나머지 (Service, Mapper, 설정 빈)
- **`ServletConfig` 에 `@Configuration` 을 붙이지 말 것.** 붙이면 루트 컨텍스트에도 등록돼
  `@EnableWebMvc` 가 두 번 적용된다.
- 설정 값은 `application.properties`(공통) 위에 `application-local.properties`(개인)가 덮어쓴다.
  **접속 계정은 개인 파일에만.** 공통 파일에 계정을 쓰면 안 된다.

## MyBatis

- **매퍼 인터페이스에는 반드시 `@Mapper`(org.apache.ibatis.annotations.Mapper) 를 붙인다.**
  `@MapperScan(annotationClass = Mapper.class)` 로 제한돼 있어 없으면 등록되지 않는다.
- 매퍼 XML 위치: `src/main/resources/mapper/<모듈>/*.xml`
- 파라미터는 **`#{}` 만** 사용한다. **`${}` 금지** (SQL Injection).
  동적 정렬 등 불가피하면 화이트리스트 검증 후 사용하고 이유를 주석에 남긴다.
- 동적 SQL 은 XML 에 쓴다. 어노테이션 SQL 은 지양한다.
- `mybatis-config.xml` 에 **`<typeAliases><package>` 를 넣지 말 것.**
  MyBatis VFS 가 Tomcat WebappClassLoader 와 충돌해 기동이 실패한다. (2026-07-31 실제 발생)
  별칭이 필요하면 `SqlSessionFactoryBean#setTypeAliasesPackage` 를 쓴다.
- 컬럼·테이블명은 `db/` 의 SQL 이 기준이다. ERD 문서와 다르면 **SQL 이 맞다.**

## 트랜잭션 · 예외

- 트랜잭션 경계는 **Service** 에 둔다. Controller·Mapper 에 `@Transactional` 금지.
- 업무 규칙 위반은 `BusinessException(code, message)` → 400 자동 변환.
- 컨트롤러에서 try-catch 로 예외를 삼키지 않는다. `CommonExceptionAdvice` 로 올린다.
- 응답은 반드시 `ApiResponse` 로 감싼다.

## 테스트

- JUnit5 + Spring Test. 기능 구현 시 동반 작성.
- 테스트 패키지는 메인과 동일하게 `com.kb.tangtang.<모듈>`.
- **실제 DB 연결이 필요한 테스트는 `@Disabled` 로 둔다.** 활성화한 채 커밋하면 팀원 빌드가 깨진다.
  (`config/DataSourceConnectionTest` 참고)

## 네이밍 (들여쓰기 4칸)

| 대상 | 규칙 | 예 |
|---|---|---|
| 클래스 | `PascalCase` | `FixedExpenseService` |
| 메서드·변수 | `camelCase` | `getUserInfo()`, `fixedExpenseList` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_USER_LIMIT` |
| 테이블 | `tbl_` 접두, snake_case | `tbl_fixed_expense` |
| 컬럼 | snake_case (camelCase 자동 매핑됨) | `merchant_name` |
| DTO | `<도메인><용도>Dto` | `FixedExpenseDetailDto` |
| 매퍼 | `<도메인>Mapper` | `FixedExpenseMapper` |
| 이벤트 | `<도메인><과거분사>Event` | `FixedExpenseDetectedEvent` |

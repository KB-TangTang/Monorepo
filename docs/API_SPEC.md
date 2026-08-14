# API 규격

모든 응답은 공통 래퍼 `com.kb.tangtang.common.dto.ApiResponse` 로 감싼다.

```
성공  { "success": true,  "data": { ... } }
실패  { "success": false, "code": "NOT_FOUND", "message": "..." }
```

인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 보낸다.
리프레시 토큰은 httpOnly 쿠키(`refresh_token`, `Path=/api/auth`)로만 오간다.

## Swagger UI, 실행 중인 서버에서 직접 확인한다

백엔드를 띄운 뒤 **<http://localhost:8080/swagger-ui.html>** 을 연다.
`@ApiOperation` 애노테이션에서 **코드가 바뀌면 문서도 함께 바뀐다.** 이 문서(`API_SPEC.md`)처럼
손으로 맞출 필요가 없다.

| 그룹 | 내용 |
|---|---|
| `01. 서비스 API` | 정식 엔드포인트 52개 |
| `02. 개발 전용 API` | `/api/dev/**`. 배치 트리거·미션 재배정. 로컬에서만 동작한다 |

**섹션은 모듈 단위 8개**다. 컨트롤러가 아니라 모듈로 묶여 있어 `01. 회원 · 인증` 안에
로그인·내 정보·동의·튜토리얼이 함께 들어간다. 태그 이름은 `common/docs/SwaggerTags` 가 소유한다.

| 섹션 | 모듈 |
|---|---|
| `00. 헬스체크` | common |
| `01. 회원 · 인증` | user (auth · users · consents · tutorial) |
| `02. 계좌 연동` | account |
| `03. 개인 미션(메인 챌린지)` | mission |
| `04. 그룹 챌린지(지방법원)` | challenge |
| `05. 알림` | notification |
| `06. 월간 리포트(판결문)` | report |
| `07. 고정지출 · 절약` | fixedexpense |

- 인증이 필요한 API 를 호출하려면 우측 상단 **Authorize** 에 `Bearer {accessToken}` 을 넣는다.
  (`Bearer ` 접두사까지 포함해야 한다. 인터셉터가 접두사를 직접 잘라낸다)
- 문서 원문(JSON): `/v2/api-docs?group=01. 서비스 API`
- `GET /api/auth/google`·`/google/callback` 은 브라우저 전체 이동이라 **Try it out 으로 확인할 수 없다.**
  `GET /api/notifications/stream` 도 SSE 라 응답이 끝나지 않아 확인할 수 없다.

문서 애노테이션은 컨트롤러가 아니라 **각 모듈의 `docs/` 패키지 인터페이스**에 있다
(`user/docs/UserControllerDocs.java` 등). 컨트롤러는 그 인터페이스를 `implements` 하기만 한다.
설명을 고칠 일이 있으면 컨트롤러가 아니라 `docs/` 쪽을 고친다.

> 이 `API_SPEC.md` 와 산출물 엑셀(`탕탕_API_연동규격_정의서`)을 **대체하지 않는다.**
> 제출 정본은 엑셀이고, Swagger 는 개발·시연·QA 용이다.
> 설정 근거는 `.claude/context/DECISIONS.md` 2026-08-13 (4) 참고.

## 고정지출 관리·절약 감정서

모든 엔드포인트는 Bearer 인증이 필요하며 사용자 ID를 요청으로 받지 않는다. 목록과 절약 감정서는
`yearMonth`를 생략하면 `Asia/Seoul` 현재월을 사용한다. 값이 있으면 `YYYY-MM` 형식의 현재월만
허용한다. 고정지출 상태 이력 스냅샷은 아직 없으므로 과거월·미래월 조회는 지원하지 않는다.

| Method | Endpoint | 응답 책임 |
|---|---|---|
| GET | `/api/fixedExpenses/candidates?yearMonth=YYYY-MM&categoryId={id}` | 현재 활성 후보·확정 목록과 일관된 관리 요약 |
| GET | `/api/fixedExpenses/candidates/{candidateId}` | 후보·확정 항목 공통 상세와 최근 6개월 결제 이력 |
| GET | `/api/fixedExpenses/savingReport?yearMonth=YYYY-MM` | 확정 활성 항목별·월간·연간 절약 가능액 |

- 목록은 페이지네이션 없이 조건에 맞는 전체 배열을 반환한다. `categoryId`는 선택값이며 요약·후보·확정
  배열에 함께 적용된다.
- 후보는 `ACTIVE`·미제외·`confirmedAt=null`, 확정 항목은 `ACTIVE`·미제외·`confirmedAt!=null`이다.
  BUFFER·검증 취소·제외 항목은 목록과 절약 감정서에서 제외한다.
- `expectedMonthlyAmount`는 목록에 포함된 후보와 확정 항목의 `averageAmount` 합계다. 절약 감정서는
  확정 항목만 `savingsAmount=averageAmount`로 계산하며, `yearlySavings=monthlySavings*12`다.
- 상세의 `paymentHistory`는 유효 소비 거래만 최신순으로 최대 6건이며, `sixMonthTotal`은 최근 6개월
  유효 결제 전체 합계다. 소유하지 않았거나 활성·미제외 상태가 아닌 항목은 `404 NOT_FOUND`다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-08",
    "summary": {
      "expectedMonthlyAmount": 86900,
      "confirmedCount": 2,
      "candidateCount": 1
    },
    "confirmed": [
      {
        "id": 101,
        "status": "ACTIVE",
        "isExcluded": false,
        "isConfirmed": true,
        "confirmedAt": "2026-08-14T10:02:03",
        "categoryCode": "subscription",
        "categoryLabel": "구",
        "name": "넷플릭스",
        "averageAmount": 17000,
        "billingCycle": { "type": "monthly", "day": 17 },
        "nextPaymentDate": "2026-08-17",
        "paymentLabel": "월 결제"
      }
    ],
    "candidates": []
  }
}
```

절약 감정서가 비어도 `200 OK`와 `monthlySavings=0`, `yearlySavings=0`, `items=[]`를 반환한다.
`yearMonth` 형식과 양수 `categoryId` 검증 실패는 `400 INVALID_REQUEST`다.

## 월간 소비 리포트

모든 엔드포인트는 Bearer 인증이 필요하며 사용자 ID를 요청 파라미터로 받지 않는다.
`yearMonth`는 `YYYY-MM` 형식이고 현재월·미래월·가입 이전 월은 상세 조회할 수 없다.

| Method | Endpoint | 응답 책임 |
|---|---|---|
| GET | `/api/reports/monthly/spending-trend?yearMonth=YYYY-MM` | 선택월을 포함한 최근 6개월 순소비 추이 |
| GET | `/api/reports/monthly/summary?yearMonth=YYYY-MM` | 당월·전월 총소비, 증감률, 활성 고정지출 후보·확정 개수 |
| GET | `/api/reports/monthly/categories?yearMonth=YYYY-MM` | 대분류 차트와 소분류 선고 명세용 순소비 정보 |
| GET | `/api/reports/monthly/months` | 가입월부터 현재월까지의 월 선택기 정보 |
| POST | `/api/reports/monthly/ai-analysis?yearMonth=YYYY-MM` | 월간 집계 스냅샷을 저장한 뒤 AI 소비 피드백·절약 비유를 수동 생성·재시도하거나 저장된 성공 결과를 재사용 |
| GET | `/api/reports/monthly/ai-analysis?yearMonth=YYYY-MM` | 저장된 AI 분석 상태·소비 피드백·절약 비유만 조회 |

`fixedExpenseCandidateCount`는 `ACTIVE`·미제외·미확정 항목 수이고, `confirmedFixedExpenseCount`는 `ACTIVE`·미제외·확정 항목 수다. 후보가 없어도 확정 항목이 있으면 절약 감정서로 이동할 수 있다.

월 선택기 응답은 다음 형태다.

```json
{
  "months": [
    {
      "value": "2026-08",
      "year": 2026,
      "month": 8,
      "available": true,
      "hasReport": false,
      "status": "ONBOARDING"
    }
  ]
}
```

`status`는 `ONBOARDING`, `FIRST_REPORT`, `READY`, `CURRENT` 중 하나다. 가입월이 아직
진행 중이면 `ONBOARDING`으로 반환하며, 이 항목은 `available=true`, `hasReport=false`다.
가입월 이후 현재월은 `CURRENT`로 반환하고 조회할 수 없다
(`available=false`, `hasReport=false`). 완료된 가입월은 `FIRST_REPORT`, 그 이후 완료월은
`READY`로 반환하며 두 상태 모두 `available=true`, `hasReport=true`다. `ONBOARDING` 월을
선택한 화면은 상세 집계 API를 호출하지 않고 온보딩 화면만 표시한다.

집계에는 `CONSUMPTION` 거래만 포함하고 `is_excluded_from_summary=1`인 거래는 제외한다.
일반 소비는 `amount`를 더하고 환불은 `COALESCE(refunded_amount, amount)`를 한 번 차감한다.
기간 조건은 월 시작일 이상, 다음 달 시작일 미만의 반개구간을 사용한다.

최근 6개월 추이는 가입월 이전 슬롯을 `{amount:null, hasData:false}`로 표시한다.
가입 이후 완료월에 소비가 없으면 `{amount:0, hasData:true}`로 표시하여 실제 0원 월과 구분한다.

증감률과 비율은 소수 둘째 자리에서 `HALF_UP`으로 반올림한다. 전월과 당월이 모두 0이면
증감률은 `0.00`, 전월이 0이고 당월이 양수이면 계산 불가이므로 `null`이다. 가입 첫 달은
`hasPreviousComparison=false`이고 전월 금액과 증감률을 `null`로 반환한다.

프론트 화면 상태는 월 목록의 `status`와 요약 응답의 `hasPreviousComparison`으로 결정한다.
`ONBOARDING`은 온보딩 화면, `FIRST_REPORT`는 전월 비교가 없는 첫 리포트,
`READY`는 일반 리포트로 표시한다. 상세 집계 응답에는 별도 상태 필드를 추가하지 않고,
`hasPreviousComparison=false`인 응답을 `FIRST_REPORT` 화면 모델로 조합한다.

카테고리 응답의 `parentCategories`는 원형 차트용 대분류별 금액·비율이고, `categories`는
선고 명세용 소분류별 금액·비율·전월 증감률이다. 각 소분류에는 `parentCategoryId`와
`parentCategoryName`을 포함한다. 대분류가 직접 지정된 거래는 해당 대분류 자체를 명세 항목으로
반환한다. 카테고리 없는 소비는 두 목록 모두 ID `null`, 이름 `"미분류"`로 반환한다. 환불 반영 후
각 분류의 순소비가 0 이하이면 해당 목록에서 제외한다. 예산 대비 분석과 단일 `categoryId` 조회는
이 API의 범위가 아니다.

AI 분석 생성은 같은 사용자·월의 `tbl_asset_snapshot.category_summary_json`에 아래 구조를 저장한다.
이는 #154의 카테고리 응답과 같은 월간 집계 스냅샷이다. 스냅샷이 없거나 AI 상태가
`NOT_REQUESTED`·`FAILED`인 경우에만 최신 집계를 저장한다. `COMPLETED` 또는 `IN_PROGRESS`인 행은
`category_summary_json`을 포함한 스냅샷을 갱신하지 않는다.

```json
{
  "yearMonth": "2026-07",
  "totalSpent": 3802832,
  "parentCategories": [
    {
      "categoryId": 1,
      "categoryName": "식비",
      "amount": 671083,
      "ratio": 17.65
    }
  ],
  "categories": [
    {
      "parentCategoryId": 1,
      "parentCategoryName": "식비",
      "categoryId": 13,
      "categoryName": "음식점/외식",
      "amount": 174649,
      "ratio": 4.59,
      "previousMonthAmount": 136374,
      "changeRate": 28.07
    }
  ]
}
```

### `POST /api/reports/monthly/ai-analysis?yearMonth=YYYY-MM`

월간 소비 집계를 바탕으로 AI 소비 피드백과 절약 비유를 생성한다. 요청 본문은 없으며 `yearMonth`는 필수 쿼리 파라미터다. 스냅샷이 없거나 상태가 `NOT_REQUESTED`·`FAILED`이면 `user_id`, `year_month`, `total_asset`, `total_debt`, `net_worth`, `category_summary_json`을 최신 값으로 저장한 뒤 생성·재시도한다. `total_asset`은 활성 연결계좌 잔액을 기준으로 하되 증권계좌는 보유 종목 평가금액을 사용해 중복 집계를 막고, `total_debt`은 대출 잔액 합계, `net_worth`는 그 차이다. `COMPLETED`면 저장된 스냅샷과 성공 결과를 그대로 반환하며, `IN_PROGRESS`면 스냅샷을 바꾸지 않고 중복 생성을 막는다.

- 인증: Bearer JWT
- 요청 본문: 없음
- `yearMonth`: `YYYY-MM` 형식, 필수
- 외부 전송 범위: 월별·카테고리별 집계 및 서버가 계산한 절감액만 허용한다. 사용자 식별 정보, 계좌·카드 정보, 거래 원문은 전송하지 않는다.
- `feedbacks`: 1~3개의 문자열만 반환한다. 각 배열 원소는 제목이나 분류 객체가 아닌 사용자에게 보여 줄 피드백 문장 하나다.
- `savingsAnalogy`: 절감액이 양수이고 전월 비교가 가능한 경우에만 문자열로 반환한다. AI는 반드시 `이번달 아낀 {절감액}원은 {실물자산} {수량}{실물자산의 단위}` 형식으로 생성한다. 예: `이번달 아낀 128,000원은 카페라떼 26잔`.
- 첫 리포트이거나 절감액이 0원이면 `savingsAnalogy`는 `null`이다.
- `status`: 성공 응답은 `COMPLETED`다. 상태와 결과는 `tbl_asset_snapshot`에 함께 저장하지만, 기존 #156의 AI 입력 집계·비식별화·외부 호출 규칙은 변경하지 않는다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-07",
    "status": "COMPLETED",
    "feedbacks": [
      "식비 지출 비중이 지난달보다 늘었어요. 자주 이용한 지출 항목을 한 번 확인해 보세요.",
      "고정지출을 제외한 소비가 줄어 이번 달 지출을 안정적으로 관리했어요."
    ],
    "savingsAnalogy": "이번달 아낀 128,000원은 카페라떼 26잔"
  }
}
```

주요 실패는 잘못된 월(`400 INVALID_REQUEST`), 조회 불가 월(`404 NOT_FOUND`), 이미 생성 중인 월(`409 AI_ANALYSIS_IN_PROGRESS`), 호출 제한(`429 TOO_MANY_REQUESTS`), 외부 AI 일시 장애(`503 AI_PROVIDER_UNAVAILABLE`)로 구분한다.

### `GET /api/reports/monthly/ai-analysis?yearMonth=YYYY-MM`

저장된 AI 분석 상태와 결과만 조회한다. 이 경로는 외부 AI 제공자·AI 생성 서비스·POST 생성 경로를 호출하지 않으며, 스냅샷 행을 새로 만들거나 갱신하지 않는다.

- 인증: Bearer JWT
- 요청 본문: 없음
- `yearMonth`: `YYYY-MM` 형식, 필수
- `COMPLETED`: `feedbacks`는 저장된 문자열 1~3개, `savingsAnalogy`는 저장된 문자열 또는 `null`
- `NOT_REQUESTED`, `IN_PROGRESS`, `FAILED`: `feedbacks`는 빈 배열, `savingsAnalogy`는 `null`
- 스냅샷 행이 없어도 정상 상태인 `NOT_REQUESTED`와 빈 결과를 `200 OK`로 반환한다.
- `ai_analysis_failure_code`, 제공자·모델·프롬프트 버전·입력 해시·원문 AI 응답은 노출하지 않는다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-07",
    "status": "COMPLETED",
    "feedbacks": [
      "식비 지출 비중이 지난달보다 늘었어요. 자주 이용한 지출 항목을 한 번 확인해 보세요.",
      "고정지출을 제외한 소비가 줄어 이번 달 지출을 안정적으로 관리했어요."
    ],
    "savingsAnalogy": "이번달 아낀 128,000원은 카페라떼 26잔"
  }
}
```

저장된 `COMPLETED` JSON이 훼손돼 결과를 안전하게 조립할 수 없는 경우에만
`503 AI_ANALYSIS_RESULT_UNAVAILABLE`을 반환한다. 이는 월간 총소비·추이·카테고리 조회 실패와
분리된 부가 데이터 오류이며, 화면은 핵심 리포트를 계속 표시한다.

## 공통

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/health` | 불필요 | `{ status, service }` |

## 인증 (이슈 #9)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/auth/google` | 불필요 | 302 → 구글 동의 화면. `oauth_state` 쿠키 발급 |
| GET | `/api/auth/google/callback` | 불필요 | 302 → 프론트. 성공 시 `/auth/callback` + `refresh_token` 쿠키, 실패 시 `/login?error=...` |
| POST | `/api/auth/refresh` | 쿠키 | `{ accessToken, user: 사용자정보, needsConsent, needsFinancialConsent, needsAccountLink }` |
| POST | `/api/auth/logout` | 쿠키 | `{"success":true,"data":null}` + 쿠키 만료 |
| GET | `/api/users/me` | Bearer | 사용자정보 |
| PATCH | `/api/users/me/name` | Bearer | 요청 `{ name }` → 갱신된 사용자정보 |
| PATCH | `/api/users/me/nickname` | Bearer | 요청 `{ nickname }` → 갱신된 사용자정보 |
| POST | `/api/users/me/profile-image` | Bearer | 요청 multipart (파트명 `file`) → 갱신된 사용자정보 |
| DELETE | `/api/users/me/profile-image` | Bearer | 갱신된 사용자정보 (기본 아바타로 되돌리기) |
| DELETE | `/api/users/me` | Bearer | 회원 탈퇴. `{"success":true,"data":null}` + 리프레시 쿠키 만료 |

**사용자정보** = `{ id, nickname, socialName, profileImageUrl, displayName, name, email, socialProvider, tutorialSeenAt, groupTutorialSeenAt }`

**이 모양은 `GET /api/users/me` · `POST /api/auth/refresh` 의 `user` · 사용자 정보를 바꾸는 모든
`PATCH`·`POST`·`DELETE` 응답이 똑같이 쓴다.** 서버는 `UserMeDto.from(UserDto, profileImageUrl)`
한 곳에서만 만든다 — 경로마다 따로 조립하면 필드를 추가할 때 한 곳을 빠뜨려 **그 경로에서만
값이 비는** 버그가 난다.

- `profileImageUrl` 은 **미설정이면 `null`** 이고 화면은 이니셜 아바타를 그린다. 값이 있으면
  서버가 조립을 끝낸 완성 URL(`/uploads/profile/{userId}/{uuid}.jpg` 형태)이다 — 화면은 URL 을
  조립하지 않으므로 로컬 저장소 → S3 전환에 프론트 수정이 없다.
- `POST /api/users/me/profile-image` 는 업로드된 이미지를 **256x256 정사각 JPEG 로 다시 구워
  저장**한다(가운데 크롭). 원본은 보관하지 않는다.
- `DELETE /api/users/me` (회원 탈퇴, `MY_01_05`)는 **물리 삭제하지 않는다.** 동의를 전건 철회하고
  (→ 연동 계좌 자동 해제) 리프레시 토큰을 전부 폐기한 뒤 식별정보를 `NULL` 로 익명화한다.
  `provider_user_id` 에 `_withdrawn_{id}` 접미사가 붙어 유니크 키가 비므로 **같은 소셜 계정으로
  재가입할 수 있다** — 재가입자는 신규 가입자와 같은 온보딩을 다시 거치며 과거 데이터는 딸려오지 않는다.
  이미 탈퇴한 계정의 재요청도 200 이다(멱등). 액세스 토큰은 만료(15분)까지 유효하다.
  (`DECISIONS.md` 2026-08-13)
- `name` 은 **실명(본인확인용)** 이고 `nickname` 은 표시명이다. 서로 다른 컬럼·다른 엔드포인트다.
- `PATCH /api/users/me/name` 은 **간편인증 화면이 인증 요청 직전에** 부른다. 같은 화면에서 받는
  생년월일·통신사·휴대폰은 여기로 오지 않는다 — 저장하지 않는 값이기 때문이다.
  (`DECISIONS.md` 2026-08-11 (4))
- 검증 규칙: 앞뒤 공백 제거 후 **2~50자**, **한글·영문·공백만**. 어기면 `INVALID_NAME`.

### 온보딩 게이트 — 순서가 정해져 있다

신규 사용자는 **`동의 → 금융동의 → 계좌연동 → 닉네임 → 홈`** 을 순서대로 통과한다.
프론트 라우터 가드는 **부팅 시 `POST /api/auth/refresh` 응답 하나만 보고** 다음 단계를 정한다 —
화면 진입마다 조회하지 않는다.

| 플래그 | true 이면 보낼 화면 | 판정 |
|---|---|---|
| `needsConsent` | 서비스 동의 | `SIGNUP` 필수 동의(약관·개인정보·금융정보) 미완료 |
| `needsFinancialConsent` | 금융(제3자 제공) 동의 | `THIRD_PARTY` 동의 미완료 |
| `needsAccountLink` | 계좌 연동 | 활성 연결 계좌가 0개 |
| — | 닉네임 설정 | `user.nickname` 이 `null` (별도 플래그 없음) |

- ⚠ **`THIRD_PARTY` 를 `needsConsent` 에 합치면 안 된다.** 그러면 계좌를 아직 연동하지 않은
  사용자가 가입 동의 화면을 영원히 벗어나지 못한다. 그래서 단계를 나눈다.
- **서버도 검사한다.** 계좌 연결 요청(`simple-auth` · `connections`)은 `THIRD_PARTY` 동의가 없으면
  **`CONSENT_REQUIRED`(400)** 로 거부한다. 계좌 연동 진입점이 셋이라 화면 순서만으로는 우회된다 —
  **2026-08-11 까지 실제로 아무도 제3자 제공 동의를 하지 않은 채 연동해 왔다.**
- `POST /api/consents` 와 철회 응답도 `{ needsConsent, needsFinancialConsent }` 를 함께 준다.
  저장 직후 게이트를 갱신하지 않으면 동의를 마쳐도 계속 동의 화면으로 되돌아간다.
  (`DECISIONS.md` 2026-08-11 (7))

### 닉네임 — 이름 3종을 헷갈리지 말 것 (이슈 #110)

| 필드 | 컬럼 | 누가 채우나 | 뜻 |
|---|---|---|---|
| `nickname` | `tbl_user.nickname` | 사용자 (온보딩·마이페이지) | 표시명. **`null` 이면 온보딩 미완료** |
| `socialName` | `tbl_user.social_name` | 가입 시 자동 (구글 `name`) | 입력창 prefill · 표시명 fallback |
| `name` | `tbl_user.name` | 사용자 (간편인증 화면) | **실명(본인확인용).** 표시명으로 쓰지 않는다 |

- **표시명 = `nickname ?? socialName`** 이고 **서버가 `displayName` 으로 계산해 내려준다.**
  화면마다 계산하면 어디선 닉네임, 어디선 구글 이름이 나온다.
- `PATCH /api/users/me/nickname` 은 **온보딩(`AU_03_01`)과 마이페이지 수정(`MY_01_03`)이 함께 쓴다.**
- 검증: 앞뒤 공백 제거 후 **1~50자**(빈 값·공백만 불가). 어기면 `INVALID_REQUEST`.
  실명보다 느슨하다 — 표시명이라 한 글자도, 숫자·기호도 막을 이유가 없다.
- **중복 검사를 하지 않는다.** 닉네임 중복 허용이 팀 결정이라 `UNIQUE` 도 걸지 않았다
  (`DECISIONS.md` 2026-08-11 닉네임 온보딩).
- **온보딩 완료 여부는 `nickname` 이 `null` 인지로만 판별한다. 별도 판별 API 를 두지 않는다.**
- 가입 시 `nickname` 은 **비워 둔다.** 구글 이름은 `social_name` 에 넣는다 —
  미리 채우면 "설정함"과 "안 함"을 구분할 별도 플래그가 필요해진다.

### 튜토리얼 완료 플래그 (이슈 #128)

| 메서드 | 경로 | 인증 | 뜻 |
|---|---|---|---|
| PATCH | `/api/main-challenge/tutorial/complete` | Bearer | 메인(개인·대법원) 튜토리얼 `MC_01_05` 완료 |
| DELETE | `/api/main-challenge/tutorial/complete` | Bearer | 다시 보기 — 완료 시각을 지운다 |
| PATCH | `/api/group-challenge/tutorial/complete` | Bearer | 그룹(지방법원) 튜토리얼 `GC_01_01` 완료 |
| DELETE | `/api/group-challenge/tutorial/complete` | Bearer | 다시 보기 |

- 네 개 모두 응답은 **갱신된 사용자정보**다. 프론트는 이 응답으로 스토어를 갱신한다.
- 저장 위치는 `tbl_user.tutorial_seen_at`(메인) · `tbl_user.group_tutorial_seen_at`(그룹).
  **`null` 이면 아직 안 본 것**이고, 프론트는 이 값만 보고 노출을 정한다.
  로그인 시 `POST /api/auth/refresh` 응답에 이미 실려 오므로 **화면 진입마다 조회하지 않는다.**
- `localStorage` 를 쓰지 않는다 — 기기를 바꾸면 튜토리얼이 다시 뜬다
  (`DECISIONS.md` 2026-08-11 RV-108 에서 기각된 대안이다).
- 클래스는 `user` 모듈(`user/controller/TutorialController`)에 있다. 경로는 챌린지 도메인이지만
  값이 사는 테이블이 `tbl_user` 라서다.

### 인증 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `UNAUTHORIZED` | 401 | Authorization 헤더 없음/형식 오류 |
| `TOKEN_EXPIRED` | 401 | 액세스 토큰 만료 — 프론트가 자동 재발급한다 |
| `INVALID_TOKEN` | 401·400 | 서명 위조·형식 오류·리프레시 토큰 없음/만료 |
| `REFRESH_TOKEN_REUSED` | 400 | 폐기된 리프레시 토큰 재사용 — 전체 토큰 폐기됨 |
| `USER_WITHDRAWN` | 400 | 탈퇴·차단 계정 |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | 400 | 구글 code↔token 교환 실패 |
| `NOT_FOUND` | 400 | `/api/users/me` 조회 시 사용자를 찾을 수 없음 (실명 갱신 대상이 탈퇴·차단 상태일 때도 이 코드다) |
| `INVALID_NAME` | 400 | `/api/users/me/name` 의 이름이 형식에 맞지 않음 (2~50자·한글/영문/공백) |
| `INVALID_REQUEST` | 400 | `/api/users/me/nickname` 의 닉네임이 비었거나 50자를 넘음 |
| `IMAGE_REQUIRED` | 400 | `/api/users/me/profile-image` 요청에 이미지가 비어 있음 |
| `IMAGE_TOO_LARGE` | 400 | 업로드 이미지가 5MB 초과 |
| `INVALID_IMAGE` | 400 | 이미지로 디코딩할 수 없는 파일 (확장자·Content-Type 은 신뢰하지 않는다) |

### 콜백 리다이렉트 error 쿼리 (`/api/auth/google/callback` 이 붙이는 값)

`cancelled`(사용자 취소) · `invalid`(state 불일치) · `failed`(교환 실패) · `withdrawn`(이용 불가 계정)

### 프론트 재발급 실패 시 error 쿼리 (`http.js`/`main.js` 가 붙이는 값)

콜백이 아니라 액세스 토큰 재발급(`/api/auth/refresh`) 실패 시 프론트가 붙인다.

`expired`(단순 세션 만료) · `security`(`REFRESH_TOKEN_REUSED` 감지 — 탈취 의심으로 전체 토큰 폐기됨)

## 동의 (이슈 #13)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/consents/catalog?scope=SIGNUP\|FINANCIAL\|CHALLENGE` | Bearer | `{ scope, termsVersion, items:[{type,required,label,termsUrl}] }` |
| POST | `/api/consents` | Bearer | `{ needsConsent }` — 본문 `{ scope, agreements:[{type,agreed}] }` |
| GET | `/api/consents/me` | Bearer | `{ items:[{type,scope,required,label,termsUrl,agreed,withdrawable,termsVersion,expiresAt}] }` |
| POST | `/api/consents/{type}/withdraw` | Bearer | `{ needsConsent }` |

동의 그룹은 3종이다.

| scope | 항목 |
|---|---|
| `SIGNUP` | 필수 `TERMS`·`PRIVACY`·`FINANCIAL_DATA` / 선택 `AI_USAGE`·`MARKETING` |
| `FINANCIAL` | 필수 `THIRD_PARTY` |
| `CHALLENGE` | 선택 `CHALLENGE` — 개인·그룹 공통 챌린지 참여 동의 |

약관 본문은 서버가 갖지 않는다. `termsUrl` 은 노션 공개 페이지이고 새 탭으로 연다.
`terms_version` 은 요청값을 무시하고 서버 카탈로그 값을 저장한다.
`needsConsent` 는 `SIGNUP` 필수 3종 기준으로만 판정한다.

`CHALLENGE` 동의는 사용자가 메인 챌린지를 처음 시작할 때 한 번 받는다. 이후에는 다시 묻지 않으며,
마이페이지 동의 관리에서 철회하거나 재동의한다. 최초 동의와 철회 후 재동의가 완료되면 커밋 이후
`ChallengeConsentAgreedEvent`를 발행해 오늘 미션이 없는 사용자에게 당일 미션 배정을 시도한다.
이미 활성인 동의를 다시 제출해도 이벤트를 중복 발행하지 않는다. 챌린지 약관 URL은 확정 전까지 `null`이다.

동의를 기록하려면 `agreed: true` 를 명시적으로 보내야 한다. `agreed` 를 생략하거나 `null` 로 보내면 원시형 기본값 `false` 로 바인딩되어, 해당 항목을 요청에 넣지 않은 것과 동일하게 미동의로 저장된다.

`FINANCIAL_DATA` 철회는 부수효과가 있다. `THIRD_PARTY` 를 **함께 철회**하고 `ConsentWithdrawnEvent` 를 발행해
**연결된 계좌를 모두 해제**한다. 또 `SIGNUP` 필수 항목이라 철회 즉시 `needsConsent=true` 가 되어
라우터 가드가 이후 모든 이동을 동의 화면으로 되돌린다 — 화면은 철회 전에 이 사실을 알려야 한다.

### 철회한 동의를 다시 켜기 (재동의)

전용 엔드포인트가 없다. **기존 `POST /api/consents` 를 그대로 재사용**한다.

- `POST /api/consents` 는 **scope 단위 전량 저장**이다(`ConsentService.submit`).
  다시 켤 항목 하나만 보내면 **같은 scope 의 나머지가 전부 꺼진다.**
  현재 상태를 함께 실어 보내야 한다 (프론트 `utils/consent.js` 의 `buildAgreeAgainPayload`).
- 그래서 `GET /api/consents/me` 가 항목마다 **`scope`** 를 내려준다.
  프론트가 타입→scope 매핑을 들고 있으면 서버의 `ConsentScope` 가 바뀔 때 어긋난다.
- `TERMS`·`PRIVACY` 는 `withdrawable=false` 라 철회 자체가 없다.

### 동의 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `CONSENT_REQUIRED_MISSING` | 400 | 필수 항목에 동의하지 않고 저장 시도 |
| `CONSENT_TYPE_INVALID` | 400 | 알 수 없는 동의 항목·그룹이거나 해당 scope 밖의 항목 |
| `CONSENT_NOT_WITHDRAWABLE` | 400 | `TERMS`·`PRIVACY` 철회 시도 (탈퇴에 해당) |
| `NOT_FOUND` | 400 | 철회할 동의 내역 없음 |

## 계좌 연동 (이슈 #12)

기준 파일은 `AccountLinkController` 이고, 프론트 `apps/web/src/api/account.js` 의 함수 13종과 1:1 이다.
사용자 식별은 전부 `@LoginUser` 로만 한다 — 요청 본문·쿼리로 `userId` 를 받는 엔드포인트는 없다.

**어느 금융 데이터 공급자(목서버 / 실 CODEF)가 붙었는지 프론트는 알지 못한다.**
`financial.client` 프로퍼티가 정하고, 화면은 `GET /api/accounts/link/auth-methods` 응답만 보고 인증 화면을 고른다.
(DECISIONS.md 2026-08-05)

### 1단계 — 기관 선택

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/institutions` | Bearer | `{ banks:[], cards:[], securities:[] }` — 각 항목 `{code,name,shortLabel,connected}` |

- `connected: true` 는 **이미 연결된 기관**이라 다시 고를 수 없다.
- 공급자가 다루지 못하는 기관은 목록에서 아예 빠진다(`supportedOrganizations`. 빈 집합이면 "제한 없음").
- **보험은 연동 범위에서 제외됐다(2026-08-06).** 되살리려면 `InstitutionCatalog` 부터 손봐야 한다.

### 2단계 — 인증

| 메서드 | 경로 | 인증 | 요청 본문 | 응답 |
|---|---|---|---|---|
| GET | `/api/accounts/link/auth-methods` | Bearer | — | `{ methods:[{type,providers:[{code,name}],requiresIdentity}] }` |
| POST | `/api/accounts/connections/simple-auth` | Bearer | `{provider, organizations:[code], userName?, birthDate?, carrier?, phoneNo?}` | 연결 응답 |
| POST | `/api/accounts/connections` | Bearer | `{credentials:[{organization,loginType,id,password}]}` | 연결 응답 |
| GET | `/api/accounts/connections/{connectionId}/auth-status` | Bearer | — | `{ status }` |
| POST | `/api/accounts/connections/{connectionId}/extra-auth` | Bearer | `{authCode}` | 연결 응답 |

연결 응답 = `{ connectionId, status, expiresInSeconds, needsExtraAuth, extraAuthType }`
(간편인증·기관 로그인이 **같은 모양**을 돌려준다. 이후 단계는 어느 경로로 왔는지 모른다.)

- `type` 이 `SIMPLE_AUTH` 면 `providers` 가 채워지고, `INSTITUTION_LOGIN` 이면 빈 배열이다.
- 본인 정보(`userName`·`birthDate`·`carrier`·`phoneNo`)는 **`requiresIdentity: true` 일 때만** 보낸다.
  목 모드에서는 화면이 형식만 검증하고 폐기해 필드가 비어 온다. 서버도 공급자에 전달만 하고 저장·로깅하지 않는다.
  - ⚠ **이름만 예외다.** 화면이 입력받은 이름은 이 요청과 별개로 `PATCH /api/users/me/name` 이
    `tbl_user.name` 에 저장한다. 이 요청의 `userName` 은 여전히 **공급자 전달용**이고 저장되지 않는다.
    (`DECISIONS.md` 2026-08-11 (4) — 목 모드는 `requiresIdentity: false` 라 이 페이로드에만 실으면
    시연 환경에서 실명이 영영 저장되지 않는다)
- **`credentials` 는 요청 처리 중에만 존재한다.** 진행 상태·DB·로그 어디에도 남지 않는다.
- 인증 승인은 프론트가 `auth-status` 를 1초 간격으로 폴링해 확인한다.

### 3단계 — 조회 진행

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/connections/{connectionId}/progress` | Bearer | `{ percent, done, institutions:[{code,name,status}] }` |

- **진행률은 서버가 계산한다. 프론트 연출이 아니다.**
- 이 엔드포인트는 **폴링될 때마다 기관을 하나씩 실제로 조회**한다(브로커·비동기 인프라가 없는 구조라 의도한 설계).
  한 기관이 실패해도 `FAILED` 로 두고 다음 기관으로 넘어간다.
- 승인(`APPROVED`) 전에는 조회를 시작하지 않고 현재 상태만 돌려준다.
- 진행 상태는 메모리 보관이고 **TTL 10분**이다. 지나면 `CONNECTION_NOT_FOUND` 다.

### 4단계 — 계좌 선택

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/connections/{connectionId}/accounts` | Bearer | `[{bankCode,bankName,shortLabel,accounts:[…]}]` |

`accounts[]` 항목 = `{accountId, bankCode, bankName, accountType, accountName, accountNoMasked, currency, balance, alreadyLinked}`

- 이 엔드포인트만 `data` 가 **배열**이다(공통 래퍼는 동일).
- `accountId` 는 아직 DB 에 없는 **목록 순번 기반 임시 식별자**다. 5단계 요청에만 쓰고 보관하지 않는다.
- `accountNoMasked` 는 서버가 마스킹한 값이다. 원본 계좌번호는 내려보내지 않는다.
- `alreadyLinked: true` 는 이미 연결된 계좌다(계좌번호 해시로 판정).

### 5단계 — 연결 저장

| 메서드 | 경로 | 인증 | 요청 본문 | 응답 |
|---|---|---|---|---|
| POST | `/api/accounts` | Bearer | `{connectionId, accountIds:[임시 id]}` | `{ linkedCount }` |

- 중복 `accountId`·이미 연결된 계좌는 **조용히 건너뛴다**. `linkedCount` 는 실제로 연결된 수다.
- 해제했던 계좌는 새 행을 만들지 않고 되살린다(UNIQUE KEY 충돌 방지).
- 하나도 연결되지 않으면 `EXTERNAL_API_ERROR` 다.

### 연결 계좌 관리

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts` | Bearer | `{ accounts:[…], nextAutoSyncAt }` |
| POST | `/api/accounts/refresh` | Bearer | `{ lastSyncAt, institutionCount, cooldownSeconds, institutions:[…] }` |
| DELETE | `/api/accounts/{accountId}` | Bearer | `{ accountId, disconnected }` |
| POST | `/api/accounts/{accountId}/resync` | Bearer | `{ accountId, syncStatus, lastSyncAt }` |

`accounts[]` 항목 = `{accountId, bankCode, bankName, shortLabel, accountName, accountNoMasked, accountType, balance, syncStatus, lastSyncAt, syncFailReason, expiresAt}`
`institutions[]` 항목 = `{bankCode, bankName, shortLabel, newTransactionCount, balance, syncStatus}`

- `nextAutoSyncAt` — 자동 동기화는 **매일 18시 배치**다. 오늘 시각이 지났으면 내일 18시를 안내한다.
- `expiresAt` — 연결 시점 **+365일**(마이데이터 재동의 주기).
- `cooldownSeconds` 는 **150 고정이고 서버가 판단한다.** 프론트가 직접 계산하지 않는다.
- `POST /refresh` 는 **공급자를 실제로 호출한다.** 기관 단위로 묶어 한 번씩 부르고,
  `institutions[].balance` 는 그 기관 계좌들의 **잔액 합계**다(첫 계좌 값이 아니다).
- `newTransactionCount` 는 거래내역 수집이 붙기 전까지 **항상 0** 이다. 값을 지어내지 않는다.
- 조회 결과 `NEED_RECONNECT` 가 되면 `AccountReconnectRequiredEvent` 가 발행되어
  `ACCOUNT_RECONNECT` 알림이 나간다(→ 「알림」 절).

### 계좌 연동 열거값

| 대상 | 값 |
|---|---|
| 인증 수단 `type` | `SIMPLE_AUTH` · `INSTITUTION_LOGIN` |
| 인증 상태 `status` | `PENDING` · `APPROVED` · `FAILED` · `EXPIRED` |
| 진행 상태 `institutions[].status` | `WAITING` · `FETCHING` · `DONE` · `FAILED` |
| 동기화 상태 `syncStatus` | `NORMAL` · `SYNCING` · `NEED_SYNC` · `FAILED` · `NEED_RECONNECT` |

`syncStatus` 값은 `tbl_connected_account.sync_status` 컬럼과 1:1 이다 (`db/schema.sql` 이 기준).

### 계좌 연동 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `INVALID_CREDENTIALS` | 400 | 인증 수단·아이디/비밀번호·인증번호·본인 정보 누락 또는 형식 오류 |
| `CONNECTION_NOT_FOUND` | 400 | `connectionId` 가 없거나 TTL(10분) 만료, 또는 남의 연결 |
| `TOKEN_EXPIRED` | 400 | **금융기관** 연결 인증 만료 — 재연동이 필요하다 |
| `AUTH_METHOD_UNSUPPORTED` | 400 | 공급자가 지원하지 않는 인증 수단 |
| `EXTERNAL_API_ERROR` | 400 | 기관·계좌 미선택, 공급자 응답 오류 |
| `EXTERNAL_API_UNAVAILABLE` | 400 | 공급자 장애·무응답 |
| `NOT_FOUND` | 400 | 연결된 계좌 없음 또는 남의 계좌 |

> ⚠ `TOKEN_EXPIRED` 는 인증 절의 액세스 토큰 만료와 **이름만 같다.** 이쪽은 HTTP 400 이라
> 프론트 `http.js` 의 자동 재발급(401 트리거)을 유발하지 않는다.

## 자산 현황 (이슈 #240)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/assets/summary?baseDate=YYYY-MM-DD` | Bearer | 순자산·전월 대비 증감·6개월 추이·구성·종류별 목록 |

`baseDate` 는 선택값이며 생략하면 오늘 날짜(`Asia/Seoul`)를 기준으로 한다. `YYYY-MM-DD` 형식이
아니면 `400 INVALID_REQUEST` 다.

- `asOf` 는 이 응답을 계산한 단일 기준 시각이다. `netWorth`·`composition`·`assetGroups`·`trend` 의
  마지막 달 값이 전부 이 시각의 라이브 잔액을 사용해 화면 카드 간 금액이 어긋나지 않는다.
- `netWorth` = 입출금 + 예적금 + 투자 + 페이머니 − 대출잔액. 연결 해제(`is_active=0`) 계좌는 제외한다.
  투자 금액은 증권계좌 `balance` 가 아니라 `tbl_investment_holding.market_value` 합계다(보유 종목이
  없으면 계좌 `balance` 로 대체). 대출은 `tbl_loan.balance` 합계이며 `composition`·`assetGroups` 에서는
  음수로 내려간다.
- `composition`(도넛차트용)·`assetGroups`(종류별 목록용)는 항상 `DEMAND_DEPOSIT`·`SAVINGS`·
  `SECURITIES`·`PAY_MONEY`·`LOAN` 5종을 고정 순서로 반환한다. 연결된 계좌가 없는 종류도
  `amount=0`·`count=0` 으로 채워 화면이 빈 배열을 따로 처리하지 않게 한다.
- 전월 비교(`previousMonthNetWorth`·`changeAmount`·`changeRate`)와 `trend`(최근 6개월, `baseDate`가
  속한 달 포함)의 과거월 값은 `tbl_asset_snapshot.net_worth`·`total_debt` 를 조회해 채운다. 이
  테이블은 아직 월간 배치가 아니라 `POST /api/reports/monthly/ai-analysis` 호출 시에만 채워지므로,
  해당 월 스냅샷이 없으면 `trend` 항목의 `netWorth`·`totalDebt`는 `null`, 전월 비교 3개 필드도 전부
  `null` 이다 — 가입 첫 달의 전월 비교를 생략하는 월간 리포트(`hasPreviousComparison=false`)와 같은
  "데이터 없음" 처리 기준이다. `trend` 의 마지막 항목(= `baseDate` 가 속한 달)만은 스냅샷 유무와
  무관하게 방금 계산한 라이브 `netWorth`·대출잔액(`totalDebt`)을 그대로 채운다.

```json
{
  "success": true,
  "data": {
    "asOf": "2026-08-15T10:30:00",
    "netWorth": 9445500,
    "previousMonthNetWorth": 9125500,
    "changeAmount": 320000,
    "changeRate": 3.51,
    "trend": [
      { "yearMonth": "2026-03", "netWorth": null, "totalDebt": null },
      { "yearMonth": "2026-08", "netWorth": 9445500, "totalDebt": 1500000 }
    ],
    "composition": [
      { "type": "DEMAND_DEPOSIT", "label": "입출금", "amount": 2066800 },
      { "type": "LOAN", "label": "대출", "amount": -1500000 }
    ],
    "assetGroups": [
      { "type": "DEMAND_DEPOSIT", "label": "입출금 계좌", "count": 2, "amount": 2066800 }
    ]
  }
}
```

## 금융 데이터 동기화 (이슈 #147)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| POST | `/api/financial-syncs` | Bearer | `{ status, syncedSources, syncedAt, collectedTransactionCount, ruleCategorizedCount, llmPendingTransactionCount, llmCategorizationStatus }` |

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "syncedSources": ["BANK", "DEPOSIT", "SECURITIES", "LOAN", "PAY_MONEY", "CARD"],
    "syncedAt": "2026-08-13T10:00:00+09:00",
    "collectedTransactionCount": 42,
    "ruleCategorizedCount": 30,
    "llmPendingTransactionCount": 12,
    "llmCategorizationStatus": "PENDING"
  }
}
```

- `collectedTransactionCount`·`ruleCategorizedCount`·`llmPendingTransactionCount` 는 전부 **이번 호출 한 번에서 upsert 된 거래 기준**이다(사용자의 누적 미분류 거래 수가 아니다).
- 저장이 끝난 뒤 규칙 1~4단계(사용자 가맹점 매핑 → 공용 가맹점 매핑 → MCC/업종명 → 키워드)로 **동기** 카테고리화한다. `ruleCategorizedCount` 는 이 응답 안에서 확정된 값이다.
- 규칙으로도 분류하지 못한 소비 거래는 이 응답 이후 **비동기**로 LLM 분류 작업(`tbl_llm_categorization_job`, 사용자별 transaction_date 오름차순 최대 20건 배치)에 등록된다. 실제 LLM 호출은 이번 범위에 포함되지 않는다 — 작업 등록까지만 한다.
- `llmCategorizationStatus`: `PENDING`(LLM 대상 거래가 있음) · `NOT_REQUIRED`(전부 규칙으로 분류됐거나 대상 자체가 없음).
- 사용자가 직접 지정한 카테고리(`category_source='USER'`)는 재동기화로 자동 분류가 절대 덮어쓰지 않는다(DB 레벨 가드).
- 환불 거래(`is_refund=1`)는 일반 거래와 동일하게 규칙 1~4단계를 적용하되, 전부 미스하면 LLM 대상에서는 제외한다. 원거래 카테고리를 그대로 물려받는(계승) 처리는 **후속 작업**이다.
- `tbl_merchant_category_map`(공용 캐시)에 대한 write-back(3·4단계 판정 결과를 다시 채워 넣는 것)은 이번 범위가 아니다 — 비어 있으면 2단계는 항상 미스로 3단계로 넘어간다.
- 정기 동기화 스케줄러, 연결된 전체 사용자 대상 자동 동기화, 증분 동기화 조회 범위 변경도 이번 범위 밖이다.

### LLM 분류 작업 처리 (후속 구현)

`tbl_llm_categorization_job`에 `PENDING`으로 등록된 작업은 별도 API 호출 없이 서버 내부 스케줄러(`LlmCategorizationScheduler`)가 주기적으로 집어 OpenAI Chat Completions API로 실제 분류를 수행한다.

- 주기: `llm.categorization.poll.fixed-delay-ms`(기본 1분). 한 번에 최대 `llm.categorization.poll.max-jobs-per-tick`(기본 20)개 작업을 처리한다.
- 분류 결과는 `category_source='LLM'`으로 `tbl_transaction.category_id`에 반영된다. 사용자 지정(`USER`) 카테고리는 기존 DB 가드로 보호된다.
- 프롬프트에 보내는 카테고리 목록은 `id`·`name`과 함께 `parentId`도 포함한다 — `parentId`가 없으면 대분류, 있으면 그 대분류에 속한 소분류라는 것을 LLM이 구분할 수 있게 한다. LLM은 확신이 서는 가장 구체적인(소분류) 레벨을 고르고, 소분류까지 확신이 안 서면 대분류만 골라도 된다.
- LLM은 각 판정마다 `confidence`(0.0~1.0)도 함께 반환해야 하며(Structured Outputs 스키마에 필수 필드로 강제), `confidence`가 `llm.categorization.confidence-threshold`(기본 0.8) 미만이거나 아예 없으면 `categoryId`가 있어도 분류 불가(null)로 취급한다(fail-closed).
- LLM이 제공된 카테고리 목록에 없는 id를 응답하면 그 거래는 반영하지 않고 건너뛴다(환각 방지).
- LLM이 **그 작업에 속하지 않은 `transactionId`**를 응답해도 반영하지 않고 건너뛴다. `updateCategory`에는 사용자 범위 조건이 없어, 이 검증이 없으면 남의 거래 카테고리를 덮어쓸 수 있다(가맹점명·적요는 외부 유입 자유 텍스트라 프롬프트 주입 통로이기도 하다).
- 작업에 속한 거래가 하나도 없으면 LLM을 호출하지 않고 즉시 `COMPLETED`로 마감한다.
- OpenAI가 `refusal`을 돌려주거나 `finish_reason`이 `stop`이 아니면(응답 잘림) 실패로 처리한다 — "0건 분류"가 정상 완료로 기록되는 것을 막는다.
- 작업 상태는 `PENDING → PROCESSING → COMPLETED`(정상 처리, 개별 거래가 분류 안 됐어도 정상 종료) 또는 `PROCESSING → FAILED`(API 호출 자체가 실패)로 전이한다. **`FAILED` 작업은 이번 범위에서 자동 재시도하지 않는다** — 후속 작업. `PROCESSING` 선점과 `FAILED` 마감은 둘 다 `LlmCategorizationJobStateService`가 `REQUIRES_NEW` 독립 트랜잭션으로 커밋한다(처리 트랜잭션이 롤백돼도 `FAILED`가 살아남아야 재실행 루프에 빠지지 않고, 선점이 먼저 커밋돼 행 잠금을 놓아야 그 `FAILED` 기록이 잠금 대기 없이 즉시 반영된다).
- `openai.api.key`는 로컬에서는 `application-local.properties`에만 둔다(팀 공용 키는 팀 채널에서 배포). 도커(`APP_ENV=docker`)에서는 `application-docker.properties`가 `${OPENAI_API_KEY}` 환경변수를 참조하며, 실제 값은 `.env` → `docker-compose.yml`을 거쳐 주입된다.
- OpenAI 호출은 전용 `RestTemplate`(`OpenAiClientConfig.openAiRestTemplate`)을 쓰며 `openai.api.connect-timeout-ms`(기본 10초)·`openai.api.read-timeout-ms`(기본 30초) 타임아웃이 걸려 있다.
- **토큰 최적화는 이번 범위 밖**(후속 작업, 2026-08-13 논의): 지금은 작업(job) 1건마다 `classify()`를 한 번씩 호출해, 매 호출마다 카테고리 전체 목록을 새로 실어 보낸다. 처리량이 늘어나면 한 틱에서 여러 job의 거래를 모아 한 번의 `classify()` 호출로 묶어(카테고리 목록을 한 번만 전송) 토큰을 아끼는 걸 고려한다 — 단, 지금의 "job 1건 = 호출 1번" 전제로 짜인 스케줄러·작업 단위 구조를 바꿔야 하는 작업이라 별도 설계가 필요하다.

## 마이페이지 (이슈 #57)

전용 엔드포인트를 새로 만들지 않았다. 아래 기존 API 를 조합해 그린다.

| 화면 | 쓰는 API |
|---|---|
| 프로필 카드 (`/my`) | `GET /api/users/me` — `{id, nickname, profileImageUrl, name, email, socialProvider}` |
| 프로필 이미지 변경 (`/my`) | `POST /api/users/me/profile-image` (업로드) · `DELETE /api/users/me/profile-image` (기본 아바타로 되돌리기) |
| 동의 관리 (`/my/consents`) | `GET /api/consents/me` · `POST /api/consents/{type}/withdraw` · `POST /api/consents` (재동의) |
| 로그아웃 | `POST /api/auth/logout` |

- `socialProvider` 는 마이페이지가 "google · 이메일" 형식으로 표시하려고 추가한 필드다.
- 등급은 화면에 표시하지 않는다 (DECISIONS.md 2026-08-06).
- 재동의 흐름은 「동의」 절의 *철회한 동의를 다시 켜기* 를 따른다.

## 오늘의 개인 미션 조회 (이슈 #160)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/missions/today` | Bearer | 로그인 사용자의 오늘 배정된 개인 미션 조회 |

응답은 `{ missionId, missionTitle, missionContent, missionType, categoryId, parentCategoryName,
categoryName, assignDate, difficultyName, targetRate, baseAmount, targetValue, result,
assignmentReason, guideMessage, streakDays }` 형태다.

- `streakDays`는 전날 미션 판정 배치가 `tbl_streak_count`에 저장한 현재 연속 성공 일수다.
- 전날 미션이 성공하면 1 증가하고 실패하면 0으로 초기화한다. 오늘 진행 중인 미션은 포함하지 않는다.

```json
{
  "success": true,
  "data": {
    "missionId": 11,
    "missionTitle": "배달 현장 급습",
    "missionContent": "오늘 배달 지출을 목표 금액 안으로 묶는다.",
    "missionType": "RELATIVE",
    "categoryId": 18,
    "parentCategoryName": "식비",
    "categoryName": "배달앱",
    "assignDate": "2026-08-12",
    "difficultyName": "NORMAL",
    "targetRate": 25,
    "baseAmount": 24000,
    "targetValue": 18000,
    "result": "PENDING",
    "assignmentReason": null,
    "guideMessage": null,
    "streakDays": 4
  }
}
```

- 오늘 날짜는 `Asia/Seoul` 기준으로 판단한다.
- 조회 API는 미션을 새로 배정하지 않는다.
- `LOW_SPENDING_NO_SPEND` 배정은 카테고리명과 배정 사유를 조합해 `guideMessage`를 생성한다.
- 일반 배정의 `guideMessage`는 `null`이다.
- 오늘 배정된 미션이 없으면 `TODAY_MISSION_NOT_FOUND`를 반환한다.

### 개인 미션 연속 성공 및 이번 주 판정 조회 (이슈 #186)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/missions/streak` | Bearer | 현재·최장 연속 성공 일수와 이번 주 미션 결과 |

응답은 `{ streakCount, longestStreakCount, weekStartDate, weekEndDate, weeklyResults }` 형태다.
`weeklyResults[]` 항목은 `{ date, result }`이며, 조회 주간은 월요일부터 일요일까지다.

- `streakCount`는 현재 이어지고 있는 연속 성공 일수다.
- `longestStreakCount`는 사용자의 역대 최장 연속 성공 일수다.
- 전날 `PENDING` 미션을 자정 배치에서 먼저 판정한 뒤 성공이면 증가하고, 실패면 0으로 초기화한다.
- 주간 결과의 `SUCCESS`, `FAIL`, `PENDING`을 화면에서 각각 인정, 기각, 오늘 수사 중으로 표시한다.

### 개인 미션 미확인 판정 조회 및 확인 (이슈 #230)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/missions/verdicts/pending` | Bearer | 확정됐지만 확인하지 않은 개인 미션 판정 1건 조회 |
| POST | `/api/missions/verdicts/{assignmentId}/acknowledge` | Bearer | 개인 미션 판정 확인 처리 |

미확인 판정 조회 응답은 `{ assignmentId, result, assignDate, categoryName, currentAmount,
targetValue, remainAmount, overAmount, points, bonusPoints, streakDays, pendingCount, transactions }`
형태다. `transactions[]` 항목은 `{ transactionId, merchantName, amount }`이다.

- `Asia/Seoul` 기준 전날 배정됐고 `SUCCESS` 또는 `FAIL`로 확정된 판정만 조회한다.
- 사용자별 하루 한 건 제약에 따라 `pendingCount`는 현재 범위에서 항상 0이다.
- 미확인 판정이 없으면 성공 응답의 `data`는 `null`이다.
- 거래 금액은 판정 배치와 동일하게 소비·출금·요약 포함 거래만 합산하고 환불은 차감한다.
- 확인 API는 최초 `result_checked_at`을 유지하는 멱등 요청이다.
- 본인 소유의 확정 판정이 아니면 `NOT_FOUND`를 반환한다.

```json
{
  "success": true,
  "data": {
    "assignmentId": 123,
    "result": "SUCCESS",
    "assignDate": "2026-08-13",
    "categoryName": "배달앱",
    "currentAmount": 9800,
    "targetValue": 12000,
    "remainAmount": 2200,
    "overAmount": 0,
    "points": 35,
    "bonusPoints": 5,
    "streakDays": 6,
    "pendingCount": 0,
    "transactions": [
      { "transactionId": 91, "merchantName": "돈까스집 배달주문", "amount": 6300 }
    ]
  }
}
```

### 개인 미션 월간 점수 및 상위 백분율 조회 (이슈 #194, #226)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/missions/monthly-score` | Bearer | 로그인 사용자의 이번 달 개인 미션 누적 점수와 상위 백분율 |

응답은 `{ yearMonth, totalScore, topPercent }` 형태다. `yearMonth`는 `Asia/Seoul` 기준 현재 월의 `YYYY-MM` 값이다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-08",
    "totalScore": 75,
    "topPercent": 15
  }
}
```

- `SUCCESS`로 확정된 미션만 점수에 포함한다. `FAIL`, `PENDING`은 0점이다.
- 기본 점수는 배정 당시 `difficulty_id`가 참조하는 `tbl_mission_difficulty.score`를 사용한다.
- 전날 미션과 해당일 미션이 모두 `SUCCESS`이면 해당일 점수에 연속 성공 보너스 5점을 더한다.
- 전날 미션이 `FAIL`이거나 배정 이력이 없으면 연속 성공으로 계산하지 않는다.
- 월간 점수는 해당 월 확정 미션 이력 전체를 다시 합산해 `tbl_monthly_ranking.total_score`에 갱신하므로 배치 재실행에도 중복 반영되지 않는다.
- `topPercent`는 해당 월 활성 사용자의 점수 순위와 전체 참여자 수를 기준으로 올림 계산한다.
- 해당 월 랭킹 행이 아직 없으면 `totalScore`는 0, `topPercent`는 `null`을 반환한다.

## 개인 미션 월간 랭킹 조회 (이슈 #209)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/missions/rankings?yearMonth=YYYY-MM` | Bearer | 해당 월 상위 10명과 내 순위 |
| GET | `/api/missions/rankings/months` | Bearer | 전체 랭킹 데이터가 있는 `YYYY-MM` 목록 |

- 월 목록 응답은 `{ "yearMonths": ["2026-08", "2026-07"] }` 형식이다.
- 프론트에서 이 목록을 기준으로 월 선택 버튼과 연도 이동을 활성화한다.

## 메인 챌린지 카테고리 분석 (이슈 #119)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/missions/categoryAnalysis` | Bearer | 최근 28일 상대형 미션 대상 소비 상위 3개 |

응답은 `{ analysisStartDate, analysisEndDate, transactionCount, relativeEligible, topCategories }` 다.
`topCategories[]` 항목은 `{ rank, categoryId, parentCategoryName, categoryName, totalAmount, transactionCount, spendingRatio, rotationAssignDate, rotationResult, missionRound }` 형태다.

- 응답의 상위 카테고리는 매일 재산정한 목록이 아니라 아직 소진 중인 현재 분석 주기의 스냅샷이다.
- `rotationAssignDate`는 현재 주기에서 해당 카테고리가 배정된 날짜다. 아직 배정 전이면 `null`이다.
- `rotationResult`는 현재 주기의 결과이며 `SUCCESS`, `FAIL`, `PENDING`, `WAITING` 중 하나다. `WAITING`은 현재 주기에 선정됐지만 아직 배정되지 않았다는 뜻이다.
- `missionRound`는 이번 배정 또는 다음 배정이 해당 카테고리의 몇 번째 수사인지를 나타낸다. 해당 카테고리로 배정된 전체 이력 수를 세고, 현재 주기에서 아직 `WAITING`이면 다음 예정 회차를 위해 1을 더한다.

- 분석 기간은 오늘을 제외한 최근 28일이다.
- 최초 상대형 미션 자격은 거래 이력이 28일 이상이고 최근 28일 정상 소비가 50건 이상일 때 획득한다.
- 최초 자격 획득 시 `tbl_user.relative_mission_qualified_at`을 기록한다. 이 값은 챌린지 동의를 철회해도 유지한다.
- 자격 획득 후에는 최근 28일 거래가 50건 미만이어도 현재 존재하는 소비로 상위 카테고리를 다시 집계한다.
- 자격 획득 후라도 최근 28일에 상대형 미션 대상 카테고리의 양수 소비가 전혀 없으면 스냅샷을 만들 수 없다.
- 미션 대상은 `tbl_mission_pool`에 `RELATIVE` 행이 존재하는 소분류다. 현재 정책은 15개다.
- 환불은 거래 건수에서 제외하고 순소비금액에서 차감한다.
- 대상 카테고리의 양수 순소비금액이 없으면 `relativeEligible=false`, `topCategories=[]`를 반환한다.
- 소비금액, 거래 건수, 카테고리 ID 순으로 정렬해 최대 3개를 반환한다.
- `spendingRatio`의 분모는 최근 28일 전체 분류 소비의 순소비금액이다.

### 상대형 미션 자동 배정 (이슈 #139)

정규 배정은 서버 스케줄러가 매일 한국 시간 00:10에 실행한다.
실행 시간은 `mission.assignment.cron`, 타임존은 `mission.assignment.zone` 설정으로 변경할 수 있다.

- 정규 배정 대상은 `CHALLENGE` 동의가 활성 상태이고 오늘 미션이 없는 활성 사용자다.
- 최초 동의 또는 철회 후 재동의 시에는 해당 사용자만 당일 즉시 배정한다.
- 서버 시작 시 한 번, 매일 00:30에 같은 조건으로 당일 누락 배정을 한 번 복구한다.
- 복구 시각은 `mission.assignment.recovery-cron`으로 변경할 수 있다.
- 동의를 철회하면 기존 미션 기록은 유지하고 이후 정규·복구 배정 대상에서 제외한다.
- 사용자 한 명의 실패가 나머지 사용자 배정을 중단하지 않으며, 다음 복구 주기에 다시 대상이 된다.

- 사용자별로 하루 한 개만 배정하며 `uk_umi_user_date`를 최종 중복 방어선으로 사용한다.
- 최신 분석 주기의 미소진 항목 중 `category_rank` 숫자가 가장 낮은 카테고리를 먼저 사용한다.
- 미소진 항목이 없으면 최근 28일 소비를 다시 분석해 새 스냅샷을 만든다.
- 직전에 같은 카테고리에서 배정한 미션을 우선 제외하되 후보가 없으면 재사용한다.
- 절감률은 배정 시점 사용자 난이도의 DB 구간에서 양 끝을 포함한 정수로 선택한다.
- `baseAmount`는 배정일 전 28일의 해당 카테고리 양수 일별 순소비 평균이다.
- 정상 상대형의 `targetValue = baseAmount × (1 - targetRate / 100)`이다.
- 계산 목표가 해당 카테고리의 최근 28일 최소 양수 단건 결제 금액보다 낮으면 목표를 올리지 않고,
  같은 카테고리의 `ABSOLUTE`이면서 `limit_price=0`인 무지출 미션으로 전환한다.
- 무지출 전환 배정은 실제 수행 목표인 `targetValue=0`으로 저장하되,
  전환 판단 근거를 추적할 수 있도록 `targetRate`, `baseAmount`, `difficultyId`는 유지한다.
- `assignmentReason=LOW_SPENDING_NO_SPEND`를 저장하여 월 1회 절대형과 구분하고 다음 안내를 표시한다.
  `평소 {카테고리} 지출이 이미 낮아 금액을 더 나누기 어려워요. 오늘은 {카테고리} 하루 쉬기에 도전해볼까요?`
- 미션 저장과 스냅샷 `assigned_date` 갱신은 하나의 사용자별 트랜잭션으로 처리한다.
- 절대형 미션 우선 배정은 후속 이슈 범위이며, 절대형 배정일에는 이 배치를 호출하지 않아야 한다.

## 그룹 챌린지 — 생성·초대·참여·조회 (이슈 #151)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| POST | `/api/group-challenges` | Bearer | `{ groupId, inviteCode }` |
| GET | `/api/group-challenges?status=` | Bearer | `ChallengeGroup[]` |
| GET | `/api/group-challenges/{groupId}` | Bearer | `ChallengeGroup` |
| GET | `/api/group-challenges/invite-codes/{inviteCode}` | Bearer | `{ challenge, joinable, reason }` |
| POST | `/api/group-challenges/{groupId}/members` | Bearer | `ChallengeGroup` |

생성 요청 본문은 `{ groupName, categoryId, limitAmount, evalType, startDate, endDate, memo }` 다.
**정원(`maxMembers`)은 받지 않는다** — 생성 화면에 입력 UI 가 없어 서버가 6 으로 고정한다.
프론트의 자유 규칙 입력값은 ERD 컬럼명에 맞춰 `memo` 로 보낸다.

`ChallengeGroup` 은 목록·상세·미리보기가 **모두 같은 한 가지 모양**이다.
`tbl_challenge_group` 전 컬럼(`memo` 포함) + 로그인 사용자 본인의
`tbl_group_member` 값(`livesCount`, `finalOutcome`, `finalRank`, `finalChargeAmount`)
+ 파생값(`totalDays`, `currentDay`, `daysUntilStart`, `maxLives`, `memberCount`,
`owner`, `member`, `joinable`) + `members[{userId, nickname, owner}]`.

> 목록/상세를 다른 모양으로 나누지 않았다. 차이가 `memo` 하나뿐인데 나누면 미리보기 응답이
> `data.challenge.challenge.groupName` 처럼 두 겹으로 접혀 프론트가 매번 풀어야 한다.

- `status` 는 콤마 또는 반복으로 여러 개를 넘긴다. 화면의 「종료됨」 탭은 `JUDGING,CLOSED` 를 함께 본다.
  값을 안 주면 전체다. 열거값에 없는 값은 빈 목록이 아니라 400 이다.
- **목숨은 참여 시점에 계산해 저장한다.** `lives_count` 가 NOT NULL 인데 기본값이 없어서다.
  DAILY = 기간 일수, PERIOD = 1. 늦게 참여해도 목숨은 같다 — 의도된 정책이다.
- **초대 코드는 5자리 대문자·숫자다.** 참여 코드 입력 UI 가 5칸이라 거기에 맞췄다.
  혼동되는 글자(`0 O 1 I L`)는 알파벳에서 뺐다.
- **초대 코드에는 만료 컬럼이 없다.** `start_date` 에서 파생한다 — 시작일 당일 23:59 까지 모집하고
  그 다음 날부터 만료다. 코드는 대소문자를 가리지 않으며 저장은 항상 대문자다.
- **제한 금액 0원은 정상 입력값이다** (무지출 챌린지). 음수만 막는다.
- 방장도 `tbl_group_member` 에 들어간다. 정원·목숨·랭킹이 전부 이 테이블을 세기 때문이다.
- 상세는 **참여자만** 볼 수 있다. 비참여자는 초대 코드 미리보기 경로를 쓴다.
- `pendingTrialCount`·`defendant` 는 `tbl_indictment` 구현 전이라 항상 `0`/`false` 다.
  절감액·채팅 필드는 근거 데이터가 없어 **아예 내려주지 않는다.**
- 상태 전이(`RECRUITING` → `ACTIVE`/`CLOSED`)와 시작 알림은 이 API 가 하지 않는다 —
  별도 배치(이슈 #152, 아래 참고). **참여 가능 판정은 날짜가 아니라 `status` 하나를 기준으로 한다.**
  그래서 배치가 밀리면 시작일이 지났어도 잠시 참여가 열려 있을 수 있다.

### 초대 코드 미리보기가 200 인 이유

코드 자체가 없으면 `GROUP_INVITE_CODE_NOT_FOUND` 로 끝내지만, **코드는 유효한데 참여만 못 하는**
경우는 200 + `joinable:false` + `reason` 으로 내려간다. 참여 확인 화면(GC_01_06)이 그룹 정보를
먼저 보여준 다음 사유를 안내해야 하기 때문이다.

| `reason` | 뜻 |
|---|---|
| `ALREADY_JOINED` | 이미 참여 중 (다른 사유보다 먼저 판정한다 — 그룹으로 보내야 하므로) |
| `CLOSED` | 상태가 `JUDGING` 또는 `CLOSED` |
| `EXPIRED` | 이미 시작됐다 (`status=ACTIVE`. 상태 전이 배치가 바꿔 놓는다) |
| `FULL` | 정원(6명) 초과 |

같은 상황에서 **참여 API** 는 200 이 아니라 400 이다 — 아래 코드로 매핑된다.

### 그룹 챌린지 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `GROUP_NAME_REQUIRED` | 400 | 이름이 비었거나 공백뿐 |
| `GROUP_NAME_TOO_LONG` | 400 | 이름 100자 초과 |
| `GROUP_LIMIT_AMOUNT_INVALID` | 400 | 제한 금액이 없거나 음수 (0원은 정상) |
| `GROUP_EVAL_TYPE_INVALID` | 400 | `DAILY`/`PERIOD` 가 아님 |
| `GROUP_PERIOD_INVALID` | 400 | 기간 누락 · 종료일이 시작일보다 앞 · 7일 초과 |
| `GROUP_START_DATE_INVALID` | 400 | 시작일이 과거 |
| `GROUP_MEMO_TOO_LONG` | 400 | 메모 300자 초과 |
| `GROUP_STATUS_INVALID` | 400 | 알 수 없는 `status` 필터 |
| `GROUP_NOT_FOUND` | 400 | 없는 그룹 |
| `GROUP_NOT_MEMBER` | 400 | 참여자가 아닌데 상세 조회 |
| `GROUP_INVITE_CODE_NOT_FOUND` | 400 | 없는 초대 코드 |
| `GROUP_INVITE_CODE_EXPIRED` | 400 | 모집 마감 후 참여 시도 |
| `GROUP_CLOSED` | 400 | 종료된 챌린지에 참여 시도 |
| `GROUP_FULL` | 400 | 정원 초과 |
| `GROUP_ALREADY_JOINED` | 400 | 이미 참여 중인데 다시 참여 시도 |
| `GROUP_INVITE_CODE_EXHAUSTED` | 400 | 초대 코드 채번 재시도 실패 (사실상 발생하지 않는다) |

## 그룹 챌린지 — 시작 상태 전이 배치 (이슈 #152)

시작일이 된 그룹의 `status` 를 서버 스케줄러가 바꾼다. **API 는 상태를 전이시키지 않는다.**

실행은 매일 한국 시간 **00:01**이다. 시각은 `challenge.group.status-transition.cron`,
타임존은 `challenge.group.status-transition.zone` 으로 바꾼다.

> **00:00 정각으로 당기지 않는다.** 기준일을 배치 안에서 `LocalDate.now(zone)` 으로 계산하는데
> 정각에는 이 값이 전날로 나올 수 있어(트리거가 미세하게 일찍 발화하거나 NTP 가 시계를 되돌림)
> 그날 시작하는 그룹이 통째로 누락된다. 미션 자동 배정(00:10)보다 앞서야 한다는 제약도 있다 —
> 미션은 챌린지가 `ACTIVE` 인 것을 전제로 한다.

- 대상은 `status=RECRUITING` 이면서 `start_date <= 기준일` 인 그룹이다.
  **등호가 아니라 부등호다** — 서버가 내려가 배치를 건너뛰었어도 다음 실행이 밀린 그룹을 주워 담는다.
- 참여자 **2명 이상**이면 `ACTIVE`, **1명(방장뿐)**이면 `CLOSED` 로 간다. 혼자서는 재판이 성립하지 않는다.
- 알림은 `ACTIVE` 면 참여자 전원에게 `GROUP_CHALLENGE_STARTED`,
  `CLOSED` 면 방장에게 `GROUP_CHALLENGE_CANCELED`. 딥링크는 `/group-challenges/{groupId}` 다.
- 트랜잭션은 **그룹 한 건 단위**다. 한 그룹이 실패해도 나머지는 처리되고, 실패분은 다음 실행이 다시 집는다.
- 멱등하다. 상태를 `RECRUITING` 인 행만 골라 UPDATE 하고 바뀐 행이 0이면 알림을 보내지 않는다.
  같은 날 두 번 돌려도 알림이 두 번 나가지 않는다.

### 배치 수동 트리거 (DEV 전용)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| POST | `/api/dev/batches/{name}?date=` | Bearer | `{ batch, baseDate, affected }` |

자정을 기다리지 않고 배치를 돌리기 위한 시연·검증용이다.

**로컬에서만 동작한다.** 판단 기준은 `app.env` 이며, 로컬은 `APP_ENV` 가 없어 기본값 `local`,
도커는 compose 가 `docker` 를 주입한다. `/api/dev/missions/**`(이슈 #165)와 같은 규칙·같은 에러 코드다.
별도 on/off 프로퍼티는 두지 않는다 — **배포 환경에서 쓸 계획이 없는 도구**라 스위치를 달면
로컬에서 쓸 때마다 설정을 고쳐야 하고, 켜진 채 배포될 위험만 새로 생긴다.

- `name` 은 현재 `group-challenge-status` 하나다. 배치가 늘어나면 여기에 추가된다.
- `date` 는 `yyyy-MM-dd`. 생략하면 오늘이다. 미래 날짜를 넣으면 그날 시작하는 챌린지까지 당겨 처리한다.
- `affected` 는 실제로 상태가 바뀐 그룹 수다.

프론트에서는 `components/dev/DevBatchTriggerFab.vue` 가 이 API 를 부른다.
**그룹 챌린지 홈**과 **재판 전체보기** 두 화면 우하단에 「챌린지 시작 배치」 버튼으로 붙어 있다
(전체보기 쪽이 본거지다 — 시작 전/진행 중/종료됨 탭이 한 화면에 있어 전이 결과가 바로 보인다).

- **짧게 누르면** 오늘 기준으로 실행한다.
- **길게 누르면(0.6초)** 기준일을 직접 넣는다. 로컬 테스트 데이터는 시작일이 대부분 미래라
  (생성 검증이 과거 시작일을 막는다) 오늘 기준으로는 대상이 잡히지 않는 경우가 흔하다.
- 결과는 버튼 라벨에 잠깐 표시하고(`2건 전이됨` / `대상 없음`), 부모 화면이 목록을 다시 불러온다.

> ⚠ 버튼은 `import.meta.env.DEV` 로 가려 **프로덕션에서 렌더링되지 않지만, 청크 자체는 번들에 들어간다**
> (`DevDataSourceFab` 도 마찬가지다). 트리셰이킹으로 사라지지 않는다 — 조건 검사가 import 지점이 아니라
> 컴포넌트 안에 있기 때문이다. **차단의 근거는 프론트가 아니라 서버의 `app.env` 다.**

| 코드 | HTTP | 상황 |
|---|---|---|
| `DEV_API_DISABLED` | 400 | 로컬 환경이 아니다 (`app.env != local`) |
| `DEV_BATCH_NOT_FOUND` | 400 | 없는 배치 이름 |

## 알림 (이슈 #58)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/notifications?cursor=&size=` | Bearer | `{ items:[{id,type,title,content,deepLinkUrl,isRead,createdAt}], nextCursor, unreadCount }` |
| GET | `/api/notifications/unread-count` | Bearer | `{ unreadCount }` |
| POST | `/api/notifications/{id}/read` | Bearer | `{ unreadCount }` |
| POST | `/api/notifications/read-all` | Bearer | `{ unreadCount }` |
| GET | `/api/notifications/stream` | Bearer | SSE. `connected` 1회 → 이후 `notification` 이벤트 |

- `cursor` 없으면 최신부터. 있으면 `id < cursor` 부터 `id DESC`
- `nextCursor` 는 마지막 항목의 `id`. 더 없으면 `null`
- `size` 기본 20 · 최대 50 (초과는 50 으로 자른다)
- SSE 는 스트림이라 `ApiResponse` 로 감싸지 않는다. 15초마다 `: ping` 주석 프레임을 보낸다
- `notification` 이벤트의 `data` 는 **목록 항목과 완전히 같은 모양**이다
  (`{id,type,title,content,deepLinkUrl,isRead,createdAt}`) — 변환은 `NotificationDto.from` 한 곳뿐
- `isRead` 는 boolean. Lombok getter 를 Jackson 이 `read` 로 바꾸지 않도록 `@JsonProperty("isRead")` 로 고정돼 있다
- `createdAt` 은 `yyyy-MM-ddTHH:mm:ss` (ISO-8601, 초 단위). 푸시된 알림도 서버가 값을 채워 내려보낸다

### 알림 종류 (`type`)

`tbl_notification.type` 컬럼(VARCHAR(30))에 그대로 들어가는 값이다.

| 값 | 기본 제목 | 발행 주체 |
|---|---|---|
| `ACCOUNT_RECONNECT` | 계좌 재연동이 필요해요 | `AccountReconnectRequiredEvent` |
| `GROUP_CHALLENGE_STARTED` | 챌린지가 시작됐어요 | challenge — 상태 전이 배치 (#152) |
| `GROUP_CHALLENGE_CANCELED` | 챌린지가 성립되지 않았어요 | challenge — 상태 전이 배치 (#152). 시작일에 참여자가 방장 1명뿐일 때 |
| `GROUP_JUDGMENT` | 판결이 확정됐어요 | challenge — 백엔드 미구현 |
| `GROUP_TRIAL_OPENED` | 재판이 열렸어요 | challenge — 백엔드 미구현 |
| `MISSION_DEADLINE` | 오늘 미션 마감 임박 | mission — 백엔드 미구현 |
| `MONTHLY_REPORT` | 판결문이 도착했어요 | report — 백엔드 미구현 |
| `PAYMENT_DUE` | 결제 예정 알림 | fixedexpense — 백엔드 미구현 |

미구현 종류는 `db/seed_notification_demo.sql` 로만 들어간다 (DECISIONS.md 2026-08-06 (3)).

### 알림 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `NOT_FOUND` | 400 | 없는 알림 또는 남의 알림 (구분해 알려주지 않는다) |

## 개인 미션 난이도 변경

`PATCH /api/users/me/difficulty`

요청은 `{ "difficultyName": "EASY" }` 형식이며 `EASY`, `NORMAL`, `HARD` 중 하나를 보낸다.
응답은 갱신된 `UserMeDto`다. 변경한 값은 이미 배정된 오늘 미션에는 소급하지 않고 다음 미션 배정부터 적용한다.

## 월간 소비 리포트

모든 엔드포인트는 Bearer 인증이 필요하며 사용자 ID를 요청 파라미터로 받지 않는다.
`yearMonth`는 `YYYY-MM` 형식이고 현재월·미래월·가입 이전 월은 상세 조회할 수 없다.

| Method | Endpoint | 응답 책임 |
|---|---|---|
| GET | `/api/reports/monthly/spending-trend?yearMonth=YYYY-MM` | 선택월을 포함한 최근 6개월 순소비 추이 |
| GET | `/api/reports/monthly/summary?yearMonth=YYYY-MM` | 당월·전월 총소비, 증감률, 활성 고정지출 후보·확정 개수 |
| GET | `/api/reports/monthly/categories?yearMonth=YYYY-MM` | 대분류 차트와 소분류 선고 명세용 순소비 정보 |
| GET | `/api/reports/monthly/months` | 가입월부터 현재월까지의 월 선택기 정보 |
| POST | `/api/reports/monthly/ai-analysis?yearMonth=YYYY-MM` | 매월 1일 00:15 KST 배치가 자동 생성하는 결과의 수동 재처리·재시도 또는 저장된 성공 결과 재사용 |
| GET | `/api/reports/monthly/ai-analysis?yearMonth=YYYY-MM` | 저장된 AI 분석 상태·소비 피드백·절약 비유 조회. 스냅샷 행이 없으면 온디맨드 생성 후 결과 반환 |

`fixedExpenseCandidateCount`는 `ACTIVE`·미제외·미확정 항목 수이고, `confirmedFixedExpenseCount`는 `ACTIVE`·미제외·확정 항목 수다. 후보가 없어도 확정 항목이 있으면 절약 감정서로 이동할 수 있다.

월 선택기 응답은 다음 형태다.

```json
{
  "months": [
    {
      "value": "2026-08",
      "year": 2026,
      "month": 8,
      "available": true,
      "hasReport": false,
      "status": "ONBOARDING"
    }
  ]
}
```

`status`는 `ONBOARDING`, `FIRST_REPORT`, `READY`, `CURRENT` 중 하나다. 가입월이 아직
진행 중이면 `ONBOARDING`으로 반환하며, 이 항목은 `available=true`, `hasReport=false`다.
가입월 이후 현재월은 `CURRENT`로 반환하고 조회할 수 없다
(`available=false`, `hasReport=false`). 완료된 가입월은 `FIRST_REPORT`, 그 이후 완료월은
`READY`로 반환하며 두 상태 모두 `available=true`, `hasReport=true`다. `ONBOARDING` 월을
선택한 화면은 상세 집계 API를 호출하지 않고 온보딩 화면만 표시한다.

집계에는 `CONSUMPTION` 거래만 포함하고 `is_excluded_from_summary=1`인 거래는 제외한다.
일반 소비는 `amount`를 더하고 환불은 `COALESCE(refunded_amount, amount)`를 한 번 차감한다.
기간 조건은 월 시작일 이상, 다음 달 시작일 미만의 반개구간을 사용한다.

최근 6개월 추이는 가입월 이전 슬롯을 `{amount:null, hasData:false}`로 표시한다.
가입 이후 완료월에 소비가 없으면 `{amount:0, hasData:true}`로 표시하여 실제 0원 월과 구분한다.

증감률과 비율은 소수 둘째 자리에서 `HALF_UP`으로 반올림한다. 전월과 당월이 모두 0이면
증감률은 `0.00`, 전월이 0이고 당월이 양수이면 계산 불가이므로 `null`이다. 가입 첫 달은
`hasPreviousComparison=false`이고 전월 금액과 증감률을 `null`로 반환한다.

프론트 화면 상태는 월 목록의 `status`와 요약 응답의 `hasPreviousComparison`으로 결정한다.
`ONBOARDING`은 온보딩 화면, `FIRST_REPORT`는 전월 비교가 없는 첫 리포트,
`READY`는 일반 리포트로 표시한다. 상세 집계 응답에는 별도 상태 필드를 추가하지 않고,
`hasPreviousComparison=false`인 응답을 `FIRST_REPORT` 화면 모델로 조합한다.

카테고리 응답의 `parentCategories`는 원형 차트용 대분류별 금액·비율이고, `categories`는
선고 명세용 소분류별 금액·비율·전월 증감률이다. 각 소분류에는 `parentCategoryId`와
`parentCategoryName`을 포함한다. 대분류가 직접 지정된 거래는 해당 대분류 자체를 명세 항목으로
반환한다. 카테고리 없는 소비는 두 목록 모두 ID `null`, 이름 `"미분류"`로 반환한다. 환불 반영 후
각 분류의 순소비가 0 이하이면 해당 목록에서 제외한다. 예산 대비 분석과 단일 `categoryId` 조회는
이 API의 범위가 아니다.

월간 리포트 배치는 매월 1일 **00:15 KST**에 정확히 이전 달만 대상으로 실행한다. `ACTIVE`이면서
대상 월의 다음 달 1일보다 전에 가입한 사용자만 처리하며, 서버 기동 시 과거 월을 일괄 보정하지 않는다.
같은 사용자·월의 `tbl_asset_snapshot.category_summary_json`에는 아래 #154 카테고리 응답 구조를 저장한다.
스냅샷이 없거나 AI 상태가 `NOT_REQUESTED`·`FAILED`인 경우에만 최신 집계를 저장한다. `COMPLETED` 또는
`IN_PROGRESS` 행은 `category_summary_json`을 포함한 스냅샷을 갱신하지 않는다.

일시 장애(`TOO_MANY_REQUESTS`, `AI_PROVIDER_UNAVAILABLE`)로 `FAILED`가 된 행은 총 3회 시도 안에서만
자동 재시도한다. 1~3일 **00:40 KST** 복구 실행은 마지막 실패 후 20분이 지난 행만 다시 처리한다.
`IN_PROGRESS`는 자동 복구하지 않아 외부 AI의 중복 호출을 피한다. AI 생성 실패는 해당 사용자의 상태만
`FAILED`로 남기며, 월간 집계·추이·카테고리 조회는 계속 가능하다.

```json
{
  "yearMonth": "2026-07",
  "totalSpent": 3802832,
  "parentCategories": [
    {
      "categoryId": 1,
      "categoryName": "식비",
      "amount": 671083,
      "ratio": 17.65
    }
  ],
  "categories": [
    {
      "parentCategoryId": 1,
      "parentCategoryName": "식비",
      "categoryId": 13,
      "categoryName": "음식점/외식",
      "amount": 174649,
      "ratio": 4.59,
      "previousMonthAmount": 136374,
      "changeRate": 28.07
    }
  ]
}
```

### `POST /api/reports/monthly/ai-analysis?yearMonth=YYYY-MM`

이 공개 경로는 월초 자동 배치를 기다릴 수 없을 때의 수동 재처리용이다. 월간 소비 집계를 바탕으로 AI 소비 피드백과 절약 비유를 생성하며, 요청 본문은 없고 `yearMonth`는 필수 쿼리 파라미터다. 스냅샷이 없거나 상태가 `NOT_REQUESTED`·`FAILED`이면 `user_id`, `year_month`, `total_asset`, `total_debt`, `net_worth`, `category_summary_json`을 최신 값으로 저장한 뒤 생성·재시도한다. `total_asset`은 활성 연결계좌 잔액을 기준으로 하되 증권계좌는 보유 종목 평가금액을 사용해 중복 집계를 막고, `total_debt`은 대출 잔액 합계, `net_worth`는 그 차이다. `COMPLETED`면 저장된 스냅샷과 성공 결과를 그대로 반환하며, `IN_PROGRESS`면 스냅샷을 바꾸지 않고 중복 생성을 막는다.

- 인증: Bearer JWT
- 요청 본문: 없음
- `yearMonth`: `YYYY-MM` 형식, 필수
- 외부 전송 범위: 월별·카테고리별 집계 및 서버가 계산한 절감액만 허용한다. 사용자 식별 정보, 계좌·카드 정보, 거래 원문은 전송하지 않는다.
- `feedbacks`: 1~3개의 문자열만 반환한다. 각 배열 원소는 제목이나 분류 객체가 아닌 사용자에게 보여 줄 피드백 문장 하나다.
- `savingsAnalogy`: 절감액이 양수이고 전월 비교가 가능한 경우에만 문자열로 반환한다. AI는 반드시 `이번달 아낀 {절감액}원은 {실물자산} {수량}{실물자산의 단위}` 형식으로 생성한다. 예: `이번달 아낀 128,000원은 카페라떼 26잔`.
- 첫 리포트이거나 절감액이 0원이면 `savingsAnalogy`는 `null`이다.
- `status`: 성공 응답은 `COMPLETED`다. 상태와 결과는 `tbl_asset_snapshot`에 함께 저장하지만, 기존 #156의 AI 입력 집계·비식별화·외부 호출 규칙은 변경하지 않는다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-07",
    "status": "COMPLETED",
    "feedbacks": [
      "식비 지출 비중이 지난달보다 늘었어요. 자주 이용한 지출 항목을 한 번 확인해 보세요.",
      "고정지출을 제외한 소비가 줄어 이번 달 지출을 안정적으로 관리했어요."
    ],
    "savingsAnalogy": "이번달 아낀 128,000원은 카페라떼 26잔"
  }
}
```

주요 실패는 잘못된 월(`400 INVALID_REQUEST`), 조회 불가 월(`404 NOT_FOUND`), 이미 생성 중인 월(`409 AI_ANALYSIS_IN_PROGRESS`), 호출 제한(`429 TOO_MANY_REQUESTS`), 외부 AI 일시 장애(`503 AI_PROVIDER_UNAVAILABLE`)로 구분한다.

### `GET /api/reports/monthly/ai-analysis?yearMonth=YYYY-MM`

스냅샷 행이 이미 있으면 저장된 AI 분석 상태와 결과만 조회한다. 다만 월초 배치가 누락돼 해당 사용자·월의 스냅샷 행이 전혀 없으면, 이 조회는 최신 스냅샷을 저장하고 AI 분석을 생성한 뒤 그 결과를 반환한다. 기존 행이 `NOT_REQUESTED`이면 상태만 반환하므로 화면은 이어서 `POST` 생성 경로를 호출한다. `FAILED`·`IN_PROGRESS`·`COMPLETED` 기존 행은 이 조회로 상태를 바꾸지 않는다.

- 인증: Bearer JWT
- 요청 본문: 없음
- `yearMonth`: `YYYY-MM` 형식, 필수
- `COMPLETED`: `feedbacks`는 저장된 문자열 1~3개, `savingsAnalogy`는 저장된 문자열 또는 `null`
- `NOT_REQUESTED`, `IN_PROGRESS`, `FAILED`: `feedbacks`는 빈 배열, `savingsAnalogy`는 `null`
- 스냅샷 행이 없으면 온디맨드 생성 결과(`COMPLETED`) 또는 생성 실패 응답을 반환한다.
- `ai_analysis_failure_code`, 제공자·모델·프롬프트 버전·입력 해시·원문 AI 응답은 노출하지 않는다.

```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-07",
    "status": "COMPLETED",
    "feedbacks": [
      "식비 지출 비중이 지난달보다 늘었어요. 자주 이용한 지출 항목을 한 번 확인해 보세요.",
      "고정지출을 제외한 소비가 줄어 이번 달 지출을 안정적으로 관리했어요."
    ],
    "savingsAnalogy": "이번달 아낀 128,000원은 카페라떼 26잔"
  }
}
```

저장된 `COMPLETED` JSON이 훼손돼 결과를 안전하게 조립할 수 없는 경우에만
`503 AI_ANALYSIS_RESULT_UNAVAILABLE`을 반환한다. 이는 월간 총소비·추이·카테고리 조회 실패와
분리된 부가 데이터 오류이며, 화면은 핵심 리포트를 계속 표시한다.

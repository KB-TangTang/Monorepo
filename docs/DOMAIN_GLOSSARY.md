# DOMAIN GLOSSARY — 도메인 용어집

> **⚠️ 아직 팀 확정 전 초안이다.** 영문 코드명은 제안값이며, 팀 합의 후 이 경고 문구를 지운다.
> 이미 코드에 등장한 이름이 있으면 **그쪽을 우선**한다.

## 왜 필요한가

"지갑재판소" 컨셉이라 한글 도메인 용어와 영문 코드명이 1:1로 붙지 않는다.
팀원 6명이 서로 다른 AI 도구(Claude Code · Codex · Gemini)를 쓰기 때문에,
매핑을 고정하지 않으면 같은 개념에 `TrialService` · `CourtService` · `JudgmentService` 가 동시에 생긴다.

**새 클래스·테이블·API 를 만들기 전에 이 표를 먼저 본다. 표에 없으면 추가하고 팀에 알린다.**

## 핵심 개념

| 한글 | 영문 코드명 | 모듈 | 테이블(예정) | 설명 |
|---|---|---|---|---|
| 재판 | `trial` | challenge | `tbl_trial` | 지출 항목을 심판하는 단위 흐름 |
| 피고인 (지출 항목) | `defendant` | fixedexpense | — | 사람이 아니라 **지출 항목**이 피고인이다 |
| 원고 (사용자) | `user` | user | `tbl_user` | 서비스 이용자 |
| 탕탕이 (마스코트·판사) | `judge` | — | — | UI 표기용. 코드에서는 판정 로직을 가리킨다 |
| 기소 (고정지출 자동 탐지) | `detection` | fixedexpense | `tbl_fixed_expense` | 반복 결제 패턴 탐지 결과 |
| 고정지출 | `fixedExpense` | fixedexpense | `tbl_fixed_expense` | 주기적으로 빠져나가는 지출 |
| 판결문 (월간 리포트) | `verdict` | report | `tbl_verdict` | 절감 성과를 정리한 월간 리포트 |
| 절감액 | `savedAmount` | report | — | 검증된 실제 절감 금액 |
| 요요 (재범) 감시 | `relapse` | report | — | 끊었던 지출이 되살아났는지 감시 |
| 절약 시뮬레이션 | `simulation` | fixedexpense | — | 해지·변경 시 예상 절감액 계산 |

## 회원 — 사용자를 가리키는 이름 3종 (헷갈리기 쉬움)

`tbl_user` 에 이름처럼 보이는 컬럼이 **셋**이다. 서로 용도가 달라 절대 섞어 쓰면 안 된다.
(`DECISIONS.md` 2026-08-11 닉네임 온보딩 · 간편인증 이름 수정 허용)

| 한글 | 영문 코드명 | 컬럼 | 누가 채우나 | 설명 |
|---|---|---|---|---|
| 닉네임 (표시명) | `nickname` | `tbl_user.nickname` | 사용자 (닉네임 설정 화면) | 화면에 보이는 이름. **미설정이면 NULL** = 온보딩 미완료 |
| 소셜 이름 | `socialName` | `tbl_user.social_name` | 가입 시 자동 (구글 `name`) | 닉네임 입력창 prefill · 온보딩 미완료 구간의 표시명 |
| 실명 | `name` | `tbl_user.name` | 사용자 (간편인증 화면) | **본인확인용.** 금융기관 인증에 쓰는 값이라 표시명으로 쓰지 않는다 |

- **표시명 규칙은 `nickname ?? socialName`** 이다. `name`(실명)은 표시명 후보가 아니다 —
  구글 이름이 실명과 다를 수 있어 한데 섞으면 계좌 인증 실명이 표시명을 덮어쓰는 사고가 난다.
- 저장 엔드포인트도 각각이다: 실명 `PATCH /api/users/me/name`, 닉네임 `PATCH /api/users/me/nickname`.
- 간편인증 화면이 함께 받는 **생년월일·통신사·휴대폰은 저장하지 않는다.** 공급자에 전달만 한다.
  이름만 예외로 저장된다.

## 리텐션 레이어

| 한글 | 영문 코드명 | 모듈 | 테이블(예정) | 설명 |
|---|---|---|---|---|
| 챌린지 | `challenge` | challenge | `tbl_challenge` | 개인·그룹 공통 상위 개념 |
| 그룹 챌린지 | `groupChallenge` | challenge | `tbl_group_challenge` | 여러 명이 참여 |
| 개인 챌린지 | `soloChallenge` | challenge | — | 혼자 수행 |
| 데일리 미션 | `dailyMission` | mission | `tbl_daily_mission` | 전일 정산형 |
| 랭킹 | `ranking` | challenge | — | 챌린지 내 순위 |

## 자산 · 거래

| 한글 | 영문 코드명 | 모듈 | 테이블(예정) | 설명 |
|---|---|---|---|---|
| 계좌 연동 | `accountLink` | account | `tbl_account` | CODEF 연동 |
| 카드 | `card` | account | `tbl_card` | |
| 거래내역 | `transaction` | transaction | `tbl_transaction` | 수집된 원본 |
| 가맹점 | `merchant` | transaction | — | 거래내역의 상호명 |
| 결제주기 | `billingCycle` | fixedexpense | — | 월·연 등 반복 주기 |
| 장부 | `ledger` | transaction | — | 사용자에게 보이는 거래 목록 화면 |

## 알림

| 한글 | 영문 코드명 | 모듈 | 테이블(예정) | 설명 |
|---|---|---|---|---|
| 알림 | `notification` | notification | `tbl_notification` | |
| 알림 실패 보관함 | `notificationDlq` | notification | `tbl_notification_dlq` | 재시도 배치 대상 |

## 표기 규칙

- **자바 클래스·필드**: 표의 영문 코드명을 camelCase/PascalCase 로 사용 (`FixedExpense`, `fixedExpenseId`)
- **테이블·컬럼**: snake_case + `tbl_` 접두 (`tbl_fixed_expense`, `fixed_expense_id`)
- **API 경로**: kebab-case 복수형 (`/api/fixed-expenses`, `/api/group-challenges`)
- **이벤트**: `<도메인><과거분사>Event` (`FixedExpenseDetectedEvent`)
- UI 문구는 한글 컨셉 용어("기소했습니다", "판결문 도착")를 쓰되, **코드에는 영문 코드명만** 쓴다

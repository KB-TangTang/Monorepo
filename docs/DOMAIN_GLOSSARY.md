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

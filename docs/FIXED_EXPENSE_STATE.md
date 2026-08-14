# 고정지출 상태 계약

## 적용 범위

- 탐지 주기는 월간만 허용한다. 주간·격주·연간 탐지는 v1 범위가 아니다.
- `tbl_fixed_expense_candidate.status`는 결제 생존·해지 검증 수명주기를 표현한다.
- `confirmed_at`은 사용자의 고정지출 확정 선택만 표현한다. 상태값으로 확정 여부를 대신하지 않는다.

## 조회 조건

| 구분 | 조건 |
|---|---|
| 탐지 후보 | `status = 'ACTIVE'`, `confirmed_at IS NULL`, `is_excluded = 0` |
| 확정 고정지출 | `status = 'ACTIVE'`, `confirmed_at IS NOT NULL`, `is_excluded = 0` |

확정 고정지출의 절약 가능액은 각 항목의 `avg_amount`를 월 금액으로 사용하고, 연간 금액은 월 금액의 12배다. 실제 절약 리포트 계산은 후속 이슈에서 구현한다.

## 상태 전이와 재발 감지

기본 검증 수명주기는 `ACTIVE → BUFFER → VERIFIED_CANCELLED`다.

- `BUFFER`는 결제 예정 시점에 일시 해지·미결제가 관찰된 항목의 기존 탐지 이력과 증거를 보존하는 상태다.
- BUFFER에서 같은 정규화 가맹점의 월간 결제가 다시 탐지되면 3개월 재탐지를 기다리지 않고 즉시 `ACTIVE`로 복귀시킨다.
- 이 재탐지 결과는 자동 확정하지 않는다. `confirmed_at`을 `NULL`로 비우고 `relapse_detected_at`을 기록해 다시 탐지 후보로 노출한다.
- `VERIFIED_CANCELLED`와 `verified_at`은 다음 검증 기간에도 결제가 없을 때의 해지 검증 완료를 뜻한다. 사용자 확정 이력이 아니다.

## 제외

`is_excluded = 1`은 사용자가 같은 정규화 가맹점 패턴을 고정지출 후보에서 제외한 기록이다. 재탐지·BUFFER 복귀 과정에서 자동으로 해제하지 않는다.

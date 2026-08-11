# db — 스키마 규칙

**이 폴더의 SQL이 스키마의 실행 기준 원본이다.** ERD 엑셀·기획 문서와 다르면 **여기 SQL이 맞다.**

## 파일

| 파일 | 용도 |
|---|---|
| `00_init_local_db.sql` | 로컬 DB·계정 생성 (관리자 계정으로 1회 실행) |
| `schema.sql` | 전체 DDL |
| `seed.sql` | 개발용 초기 데이터 |
| `seed_notification_demo.sql` | 알림 화면 시연용 데이터 (선택, 팀 공용 seed와 분리) |
| `migration/YYYYMMDD_<변경요약>.sql` | 스키마 변경분. **`schema.sql` 에 반영하지 않는 것이 원칙** |

## 신규 설치 순서

`00_init_local_db.sql` → `schema.sql` → `seed.sql` → **`migration/` 을 파일명(날짜)순으로 전부**

**마지막 단계를 빼면 컬럼이 모자란 DB 가 된다.** `schema.sql` 은 팀에 공유된 2026-08-03 이후로
갱신하지 않으므로, 그 뒤의 변경은 전부 `migration/` 에만 있다.

> ⚠ 예외 하나 — `20260805_add_account_name_to_connected_account.sql` 은 `schema.sql` 에도 반영돼 있어
> 신규 설치에서 실행하면 `Duplicate column name` 으로 죽는다. **건너뛴다.**
> 원칙이 정착되기 전에 만들어진 파일이라 그렇다. 이후 마이그레이션은 전부 실행 대상이다.

## 규칙

- 테이블 `tbl_` 접두, 컬럼 snake_case (MyBatis 가 camelCase 로 자동 매핑)
- 문자셋 `utf8mb4` / 콜레이션 `utf8mb4_general_ci`
- **이미 팀에 공유된 스키마는 `schema.sql` 을 직접 고치지 말고 `migration/` 에 변경분을 추가한다.**
  `schema.sql` 헤더에도 같은 문구가 있다. **양쪽에 같은 DDL 을 두면 신규 설치에서 중복 컬럼 오류가 난다** —
  DDL 은 항상 한 곳에서만 관리한다.
- **새 마이그레이션 머리말에 신규 설치 시 실행 여부를 적는다.** 신규 합류자가 어느 파일을 돌려야 할지
  파일만 열어보면 알 수 있어야 한다.
- 스키마를 바꿀 때 반영 순서: **SQL → 매퍼 XML → 문서**
- **계정·비밀번호를 SQL 에 넣지 않는다. 예외 없다.** `00_init_local_db.sql` 은 비밀번호 자리에
  `CHANGE_ME_DB_PASSWORD` 플레이스홀더를 두며, 각자 실행 직전에 자기 값으로 바꿔 쓰고 커밋하지 않는다.
  같은 값을 `.env`(`MYSQL_PASSWORD`) 와 `application-local.properties`(`jdbc.password`) 에도 넣어야 한다.

# db — 스키마 규칙

**이 폴더의 SQL이 스키마의 실행 기준 원본이다.** ERD 엑셀·기획 문서와 다르면 **여기 SQL이 맞다.**

## 파일

| 파일 | 용도 |
|---|---|
| `00_init_local_db.sql` | 로컬 DB·계정 생성 (관리자 계정으로 1회 실행) |
| `schema.sql` | 전체 DDL |
| `seed.sql` | 개발용 초기 데이터 |
| `seed_notification_demo.sql` | 알림 화면 시연용 데이터 (선택, 팀 공용 seed와 분리) |
| `migration/YYYYMMDD_<변경요약>.sql` | 스키마 변경분 |

## 규칙

- 테이블 `tbl_` 접두, 컬럼 snake_case (MyBatis 가 camelCase 로 자동 매핑)
- 문자셋 `utf8mb4` / 콜레이션 `utf8mb4_general_ci`
- **이미 팀에 공유된 스키마는 `schema.sql` 을 직접 고치지 말고 `migration/` 에 변경분을 추가한다.**
- 스키마를 바꿀 때 반영 순서: **SQL → 매퍼 XML → 문서**
- **계정·비밀번호를 SQL 에 넣지 않는다. 예외 없다.** `00_init_local_db.sql` 은 비밀번호 자리에
  `CHANGE_ME_DB_PASSWORD` 플레이스홀더를 두며, 각자 실행 직전에 자기 값으로 바꿔 쓰고 커밋하지 않는다.
  같은 값을 `.env`(`MYSQL_PASSWORD`) 와 `application-local.properties`(`jdbc.password`) 에도 넣어야 한다.

# db

**이 폴더의 SQL이 스키마의 실행 기준 원본이다.**
ERD 엑셀·문서와 내용이 다르면 **여기 SQL이 맞다.**

| 파일 | 용도 |
|---|---|
| `schema.sql` | 전체 DDL (테이블·인덱스·제약) |
| `seed.sql` | 개발용 초기 데이터 |
| `migration/` | 스키마 변경분. `YYYYMMDD_<변경요약>.sql` 형식 |

## 로컬 세팅

```sql
CREATE DATABASE tangtang DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
```bash
mysql -u root -p tangtang < db/schema.sql
```

## 규칙
- 테이블명은 `tbl_` 접두, 컬럼은 snake_case (MyBatis 가 camelCase 로 자동 매핑)
- 스키마를 바꿀 때는 **SQL 먼저 → 매퍼 XML → 문서** 순서로 반영한다
- 이미 공유된 스키마를 고칠 때는 `schema.sql` 을 직접 수정하지 말고 `migration/` 에 변경분을 추가한다

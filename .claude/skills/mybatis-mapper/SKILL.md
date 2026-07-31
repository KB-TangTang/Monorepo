---
name: mybatis-mapper
description: MyBatis 매퍼 인터페이스·XML을 추가하거나 수정하는 절차. @Mapper 강제, #{} 사용, XML 위치 규칙을 검사한다. "매퍼 만들어줘", "쿼리 추가", "MyBatis" 관련 요청 시 사용.
---

# MyBatis 매퍼 작성 절차

## 필수 규칙 (위반 시 동작하지 않거나 보안 취약)

1. **인터페이스에 `@Mapper`** (`org.apache.ibatis.annotations.Mapper`)
   `@MapperScan(annotationClass = Mapper.class)` 로 제한돼 있어, 없으면 빈으로 등록되지 않는다.
2. **XML 위치**: `apps/api/src/main/resources/mapper/<모듈>/<도메인>Mapper.xml`
   `mapperLocations = classpath:/mapper/**/*.xml` 로 잡혀 있다.
3. **`namespace` = 매퍼 인터페이스 FQCN**
4. **파라미터는 `#{}` 만.** `${}` 는 SQL Injection 이라 금지.
   동적 정렬 컬럼처럼 불가피하면 화이트리스트로 검증한 뒤 사용하고 이유를 주석에 남긴다.
5. **`resultType` 은 FQCN.** `mybatis-config.xml` 에 typeAlias 를 등록하지 않는다
   (Tomcat WebappClassLoader 와 충돌해 기동 실패한 이력 있음 — 2026-07-31).
6. 컬럼명은 `db/` 의 SQL 이 기준. ERD 문서와 다르면 SQL 이 맞다.
   컬럼 snake_case → 필드 camelCase 는 `mapUnderscoreToCamelCase` 로 자동 매핑된다.

## 작성 순서

1. `db/` 의 스키마에서 대상 테이블·컬럼을 확인한다. **추측 금지.**
2. 인터페이스 작성 (`<모듈>/mapper/XxxMapper.java`)
3. XML 작성 (`resources/mapper/<모듈>/XxxMapper.xml`)
4. 동적 SQL 은 `<if>` `<where>` `<foreach>` 로 XML 안에서 처리
5. Mapper 테스트 작성. DB 연결이 필요하면 `@Disabled` 로 둔다

## 템플릿

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.kb.tangtang.fixedexpense.mapper.FixedExpenseMapper">

    <select id="findByUserId" resultType="com.kb.tangtang.fixedexpense.dto.FixedExpenseDto">
        SELECT fixed_expense_id, merchant_name, amount, billing_cycle
          FROM tbl_fixed_expense
         WHERE user_id = #{userId}
        <if test="active != null">
          AND is_active = #{active}
        </if>
    </select>

</mapper>
```

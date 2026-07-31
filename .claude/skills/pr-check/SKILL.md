---
name: pr-check
description: PR 올리기 전 셀프 점검. 금지기술·시크릿·커밋 컨벤션·테스트 동반 여부를 검사한다. "PR 올리기 전", "커밋 전 점검", "푸시해도 되나" 요청 시 사용.
---

# PR 전 셀프 점검

`git diff origin/dev...HEAD` (또는 스테이징된 변경)을 대상으로 아래를 순서대로 확인하고,
**통과/실패 체크리스트로 출력**한다. 하나라도 실패면 PR을 올리지 말라고 명확히 말한다.

## 1. 금지기술 (감점 직결 — 최우선)
변경된 파일에서 아래가 등장하는지 확인:
- `spring-boot`, `@SpringBootApplication`, `application.yml` 자동설정
- `@Entity`, `JpaRepository`, `hibernate`, `jpa`
- `react`, `jsx`, `next` (프론트 의존성·코드)

→ 하나라도 발견되면 **즉시 실패 처리**하고 대체 기술(Spring Legacy / MyBatis / Vue3)을 안내한다.

## 2. 시크릿
- `application-local.properties`, `*.key`, `.env` 가 diff에 포함됐는지
- 코드·설정에 하드코딩된 비밀번호·API 키·CODEF 인증정보 (`password=`, `secret`, `apiKey` 패턴)
- `.idea/` 등 개인 IDE 설정

## 3. 코드 규칙
- 매퍼 XML 에 `${}` 사용 여부 → 발견 시 실패 (`#{}` 로 교체)
- 매퍼 인터페이스에 `@Mapper` 누락 여부
- 컨트롤러가 `ApiResponse` 로 감싸지 않고 raw 객체를 반환하는지
- `@Transactional` 이 Controller·Mapper 에 붙어 있는지
- Vue 컴포넌트에서 `axios` 직접 import, 색상 HEX 하드코딩
- DB 연결 테스트의 `@Disabled` 가 풀린 채 커밋되는지

## 4. 테스트
- 새 Service·Mapper 가 추가됐는데 테스트가 없는지
- `./gradlew :apps:api:test` 통과 여부 (실행해서 확인)

## 5. 커밋 · 브랜치 · PR
- 커밋이 `type: 한국어 요약` 형식인지.
  허용 타입: `feat` `fix` `docs` `style` `refactor` `test` `chore` `design` `comment` `rename` `remove` `!HOTFIX`
- 브랜치명이 `feature/{이슈번호}-{도메인}-{상세}` 또는 `fix/{이슈번호}-{상세}` 형식인지
- PR 대상 브랜치가 **`dev`** 인지 (`main` 직접 PR 금지)
- PR 제목이 `[타입] 제목` 형식인지

## 6. 규칙 변경 동반 여부
스택·구조·규칙을 바꾸는 PR이면 `AGENTS.md` 도 함께 수정했는지 확인한다.
(`CLAUDE.md`·`GEMINI.md` 는 포인터이므로 고치지 않는다)

## 출력 형식
```
✅ 금지기술    통과
❌ 시크릿      apps/api/.../application-local.properties 가 포함됨 → git rm --cached 필요
...
결론: PR 보류 — 위 1건 해결 후 재점검
```

# AI 도구 사용 가이드

우리 팀은 AI 코딩 도구가 섞여 있다. **Claude Code 3명 · Codex 2명 · Gemini CLI 1명.**
도구마다 읽는 규칙 파일 이름이 달라서, 그대로 두면 규칙을 3벌 관리해야 하고 3주 뒤엔 서로 다른 말을 하게 된다.

그래서 **`AGENTS.md` 하나만 원본으로 두고, 나머지는 그걸 불러오는 포인터**로 만들었다.

---

## 한 장 요약

| 당신이 쓰는 도구 | 자동으로 읽는 파일 | 실제로 적용되는 규칙 |
|---|---|---|
| Claude Code | `CLAUDE.md` | → `@AGENTS.md` 임포트 → **AGENTS.md** |
| Codex | `AGENTS.md` | → **AGENTS.md** (그대로) |
| Gemini CLI | `GEMINI.md` + `.gemini/settings.json` | → `@AGENTS.md` 임포트 → **AGENTS.md** |

**결론: 어떤 도구를 쓰든 같은 규칙이 적용된다. 규칙을 고칠 일이 있으면 `AGENTS.md`만 고친다.**

---

## 파일별 설명

### 📌 `AGENTS.md` — 규칙 원본 (루트)

프로젝트 전체에 적용되는 규칙. AI가 코드를 쓰기 전에 항상 읽는다.

담긴 내용: 금지 기술(Spring Boot·JPA·React), 고정 스택과 버전, 폴더 구조, 빌드·실행 명령어,
작업 프로토콜, API 응답 규칙, Git·커밋 규칙, 커밋 금지 목록.

- ✅ **규칙 변경은 여기에만** 한다
- ❌ 기획 배경, 평가 기준, 주차별 일정은 넣지 않는다 (코드 작성에 안 쓰이면서 AI 컨텍스트만 잡아먹는다)
- 목표 길이 130줄 이내. 길어지면 폴더별 AGENTS.md로 내려보낸다

### 📌 `CLAUDE.md` · `GEMINI.md` — 포인터 (루트)

내용은 `@AGENTS.md` 한 줄이 전부다. **여기에 규칙을 쓰지 말 것.**
심볼릭 링크를 안 쓰고 임포트로 만든 이유: 팀 전원 Windows인데 Git 기본 설정에서 심볼릭 링크가
텍스트 파일로 깨져서 체크아웃되기 때문이다.

### 📌 `apps/api/AGENTS.md` — 백엔드 전용

그 폴더에서 작업할 때만 추가로 적용된다. (루트 규칙은 이미 적용된 상태)

모듈 9개 구조, 모듈 경계 규칙(Spring Event), MyBatis 규칙, 트랜잭션·예외 처리, 네이밍.
**우리가 실제로 밟은 지뢰 두 개도 여기 적혀 있다** — `ServletConfig`에 `@Configuration` 붙이지 말 것,
`mybatis-config.xml`에 `typeAliases` 넣지 말 것. 안 적어두면 누군가 반드시 다시 밟는다.

### 📌 `apps/web/AGENTS.md` — 프론트 전용

Composition API + `<script setup>` 고정, API 호출은 `src/api/http.js` 경유,
디자인 토큰(색상 HEX 하드코딩 금지), Pinia·라우팅 규칙.

### 📌 `db/AGENTS.md` — 스키마 규칙

"`db/`의 SQL이 실행 기준 원본이다. ERD 문서와 다르면 SQL이 맞다."
마이그레이션 파일 네이밍, 스키마 변경 순서(SQL → 매퍼 → 문서).

### 📌 `docs/DOMAIN_GLOSSARY.md` — 도메인 용어집

**효과 대비 가장 중요한 파일.** 재판 컨셉이라 한글 용어와 영문 코드명이 1:1로 안 붙는다.
6명이 서로 다른 AI로 각자 번역하면 `TrialService` · `CourtService` · `JudgmentService`가 동시에 생긴다.

> ⚠️ 지금은 **초안**이다. 영문 코드명에 이견 있으면 말해달라. 확정되면 상단 경고를 지운다.

### 📌 `.claude/skills/` — 반복 작업 절차 (Claude Code 전용)

Claude Code에서 `/스킬이름`으로 호출하거나, 관련 요청을 하면 자동으로 발동한다.

| 스킬 | 언제 |
|---|---|
| `pr-check` | PR 올리기 전. 금지기술·시크릿·`${}`·커밋 컨벤션 자동 점검 |
| `api-endpoint` | 새 REST API 추가. DTO→Mapper→XML→Service→Controller→테스트 순서 강제 |
| `mybatis-mapper` | 매퍼 추가·수정. `@Mapper` 누락, `${}` 사용 방지 |
| `vue-view` | 새 화면 추가. api→store→view→router 순서, 디자인 토큰 강제 |
| `module-event` | 모듈 간 Spring Event 추가. 네이밍·발행 위치·`@Async` 주의점 |

Codex·Gemini 사용자는 이 폴더를 직접 읽으면 된다. 절차가 마크다운으로 적혀 있어 그대로 따를 수 있다.

### 📌 `.claude/settings.json` · `.gemini/settings.json` — 도구 설정 (커밋 대상)

`.claude/settings.json`은 팀 공유 권한이다. 빌드·테스트·git 조회는 승인 없이 실행되고,
`rm -rf`·force push·시크릿 파일 읽기는 차단된다.
`.gemini/settings.json`은 Gemini가 `AGENTS.md`를 읽도록 지정한다.

---

## 개인 설정은 어디에 두나

팀 규칙을 개인 파일에 쓰지 말 것. "제 로컬에선 되는데요"의 AI 버전이 그대로 재현된다.

| 파일 | 도구 | 커밋 | 용도 |
|---|---|---|---|
| `CLAUDE.local.md` | Claude Code | ✗ | 내가 맡은 파트, 개인 메모 |
| `AGENTS.override.md` | Codex | ✗ | 임시로 규칙 무시할 때만 |
| `~/.claude/CLAUDE.md` | Claude Code | – | 모든 프로젝트 공통 개인 습관 |
| `~/.codex/AGENTS.md` | Codex | – | 개인 전역 규칙 |
| `~/.gemini/GEMINI.md` | Gemini | – | 개인 전역 규칙 |

---

## 작업 흐름 예시

**"고정지출 목록 API 만들어줘"** 라고 했을 때 AI가 밟는 경로:

```
1. AGENTS.md          → 금지기술·스택·응답 래퍼 규칙 파악
2. apps/api/AGENTS.md → 어느 모듈인지(fixedexpense), @Mapper 필수, ${} 금지
3. DOMAIN_GLOSSARY.md → 영문 코드명 fixedExpense, 테이블 tbl_fixed_expense
4. api-endpoint 스킬  → DTO→Mapper→XML→Service→Controller→테스트 순서대로
5. pr-check 스킬      → 올리기 전 셀프 점검
```

이 경로가 6명 모두에게 동일하게 적용되는 것이 이 세팅의 목적이다.

---

## 지켜야 할 3가지

1. **규칙 수정은 `AGENTS.md`에만.** `CLAUDE.md`·`GEMINI.md`는 포인터다.
2. **스택·구조·규칙을 바꾸는 PR이면 `AGENTS.md`도 같이 고친다.** PR 템플릿에 체크박스가 있다.
3. **틀린 규칙은 지운다.** 코드와 안 맞는 규칙이 남아 있는 게 규칙이 없는 것보다 나쁘다.
   금요일 수행일지 쓸 때 한 번씩 훑어보자.

<!--
  PR 제목 형식: [타입] 제목   (예: [feat] 고정지출 탐지 API 구현 #12)
  대상 브랜치: dev   |   리뷰어 1명 이상 승인 후 merge
-->

## 📌 관련 이슈
- close #이슈번호

## ✨ 작업 내용
<!-- 어떤 변경 사항이 있었는지 주요 내용을 적어주세요. -->
- 고정지출 탐지 룰 기반 매칭 로직 추가
- `FixedExpenseMapper` 및 매퍼 XML 작성
- 탐지 완료 시 `FixedExpenseDetectedEvent` 발행

## 📸 스크린샷 / 테스트 결과
<!-- API 응답 결과(Postman/Swagger)나 실행 결과 스크린샷을 첨부해주세요. -->
- `GET /api/fixed-expenses` 성공 응답 확인 완료

## 🔍 리뷰 포인트
<!-- 리뷰어가 집중해서 봐주었으면 하는 부분이 있다면 적어주세요. -->
- 탐지 임계치 기준이 적절한지 봐주세요
- 트랜잭션 경계가 Service 에 올바로 잡혔는지 확인 부탁드립니다.

## ✅ 체크리스트
- [ ] 커밋 메시지 컨벤션(`type: 한국어 요약`)과 브랜치 규칙(`feature/{이슈번호}-...`)을 준수했는가?
- [ ] 로컬에서 빌드 및 테스트가 성공했는가? (`./gradlew :apps:api:build`)
- [ ] PR 대상 브랜치가 `dev` 인가?
- [ ] 불필요한 주석이나 console.log를 제거했는가?
- [ ] **시크릿(DB 계정 · CODEF 키 · `application-local.properties` · `.idea/`)이 포함되지 않았는가?**
- [ ] 매퍼에 `${}` 대신 `#{}` 를 썼고, 매퍼 인터페이스에 `@Mapper` 를 붙였는가?
- [ ] API 응답을 공통 래퍼 `ApiResponse` 로 감쌌는가?
- [ ] **스택·구조·규칙을 바꾼 PR이라면 `AGENTS.md` 도 함께 수정했는가?**

> 💡 Claude Code 사용자는 `/pr-check` 스킬로 위 항목을 자동 점검할 수 있습니다.
---
name: api-endpoint
description: 새 REST 엔드포인트를 추가하는 절차. Controller→Service→Mapper→XML→테스트 순서와 공통 규칙을 강제한다. "API 추가", "엔드포인트 만들어줘", "~조회 기능 만들어줘" 요청 시 사용.
---

# REST 엔드포인트 추가 절차

## 0. 시작 전
- `docs/DOMAIN_GLOSSARY.md` 에서 이 기능의 **영문 코드명**을 확인한다. 없으면 추가 제안 후 진행.
- 같은 모듈에 유사 엔드포인트가 이미 있는지 찾아본다. 있으면 그 구조를 따른다.
- 어느 모듈에 속하는지 정한다 (`user` `account` `transaction` `fixedexpense` `mission` `challenge` `report` `notification`).

## 1. DTO — `<모듈>/dto/`
- 요청 `<기능>RequestDto`, 응답 `<도메인><용도>Dto`
- 엔티티를 그대로 노출하지 않는다.

## 2. Mapper 인터페이스 — `<모듈>/mapper/`
```java
@Mapper                       // ← 없으면 스캔되지 않는다
public interface XxxMapper {
    XxxDto findById(Long id);
}
```

## 3. 매퍼 XML — `src/main/resources/mapper/<모듈>/XxxMapper.xml`
- `namespace` 는 매퍼 인터페이스 FQCN
- 파라미터는 **`#{}` 만**. `${}` 금지
- `resultType` 은 FQCN 으로 쓴다 (typeAlias 미사용)

## 4. Service — `<모듈>/service/`
- 트랜잭션 경계는 **여기**. 조회는 `@Transactional(readOnly = true)`
- 업무 규칙 위반은 `throw new BusinessException("CODE", "메시지")`
- 다른 모듈의 Service 를 직접 호출하지 않는다. 상태 전파가 필요하면 Spring Event (`module-event` 스킬 참고)

## 5. Controller — `<모듈>/controller/`
```java
@RestController
@RequestMapping("/api/fixed-expenses")   // kebab-case 복수형
public class XxxController {
    @GetMapping("/{id}")
    public ApiResponse<XxxDto> get(@PathVariable Long id) {
        return ApiResponse.ok(service.find(id));   // ← 반드시 래핑
    }
}
```
- try-catch 로 예외를 삼키지 않는다. `CommonExceptionAdvice` 로 올린다.

## 6. 테스트 — `src/test/java/com/kb/tangtang/<모듈>/`
- Service 단위 테스트 필수
- DB 연결이 필요한 테스트는 `@Disabled` 로 둔다

## 7. 문서
`docs/API_SPEC.md` 에 엔드포인트·요청/응답 예시를 추가한다.

## 8. 프론트 연동이 함께 필요하면
`src/api/<도메인>.js` 에 호출 함수를 추가한다. 인터셉터가 래퍼를 벗기므로 payload 를 바로 반환한다.

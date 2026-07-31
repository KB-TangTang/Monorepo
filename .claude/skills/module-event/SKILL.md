---
name: module-event
description: 모듈 간 상태 전파를 위한 Spring Event 발행·구독을 추가하는 절차. 이벤트 네이밍·발행 위치·@Async 설정 규칙을 강제한다. "이벤트 추가", "모듈 연결", "알림 보내기" 요청 시 사용.
---

# Spring Event 추가 절차

## 왜 이벤트인가
모듈 간 **직접 Service 호출을 최소화**하기 위해서다. 메시지 브로커는 쓰지 않는다(Kafka 제거됨).
단일 프로세스 안에서 `ApplicationEventPublisher` 로 연결한다.

예) 고정지출 탐지 완료 → `FixedExpenseDetectedEvent` → `report`·`notification` 이 각각 수신

## 1. 이벤트 클래스 — 발행하는 모듈의 `<모듈>/event/`
- 이름은 **`<도메인><과거분사>Event`** (`FixedExpenseDetectedEvent`, `ChallengeClosedEvent`)
- **불변으로 만든다.** final 필드 + 생성자만. setter 금지.
- 수신 측이 필요한 최소 정보만 담는다. 엔티티 통째로 넣지 않는다.

```java
public class FixedExpenseDetectedEvent {
    private final Long userId;
    private final Long fixedExpenseId;
    private final int detectedCount;
    // 생성자 + getter
}
```

## 2. 발행 — 발행 모듈의 Service
```java
private final ApplicationEventPublisher publisher;

@Transactional
public void detect(Long userId) {
    // ... 도메인 로직 ...
    publisher.publishEvent(new FixedExpenseDetectedEvent(userId, id, count));
}
```
- **발행은 Service 안에서.** Controller·Mapper 에서 발행하지 않는다.
- 트랜잭션 커밋 후에 처리돼야 하면 수신 측에서 `@TransactionalEventListener` 를 쓴다.

## 3. 구독 — 수신 모듈의 `<모듈>/listener/`
```java
@Component
public class FixedExpenseDetectedListener {

    @Async
    @EventListener
    public void onDetected(FixedExpenseDetectedEvent event) {
        // 알림 발송 / 리포트 갱신
    }
}
```
- 수신자는 **여러 모듈에 있어도 된다.** 발행자는 누가 듣는지 몰라야 한다.
- `@Async` 를 쓰면 예외가 호출자에게 전파되지 않는다. **리스너 안에서 try-catch 로 처리**하고 실패를 기록한다.
- 알림 발송 실패는 `tbl_notification_dlq` 에 적재하고 스케줄 배치가 재시도한다.

## 4. 확인
- 발행 모듈과 수신 모듈이 **서로 import 하지 않는지** 확인한다. 이벤트 클래스만 공유된다.
- 이벤트 클래스가 여러 모듈에서 쓰이면 `common/event/` 로 옮기는 것을 검토한다.
- 리스너 단위 테스트를 작성한다.

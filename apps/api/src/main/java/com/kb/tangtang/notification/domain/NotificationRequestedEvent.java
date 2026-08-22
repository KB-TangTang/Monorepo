package com.kb.tangtang.notification.domain;

import java.util.Map;

/**
 * 알림을 보내 달라는 요청. **모든 도메인이 쓰는 단 하나의 알림 이벤트다** (이슈 #68).
 *
 * 이전에는 알림 종류마다 notification 모듈에 리스너를 추가해야 했다. 그래서 챌린지·미션 담당자가
 * 알림을 붙이려면 매번 남의 모듈을 고쳐야 했고, 6명이 병렬로 붙이는 구조에서 병목이 됐다.
 * 이 이벤트 하나만 발행하면 notification 모듈은 건드리지 않는다.
 *
 * 문구 자체는 {@link NotificationType} 이 소유한다 — 여기에는 **치환값만** 싣는다
 * (팀 결정 2026-08-07. 문구가 6개 모듈에 흩어지면 톤이 제각각이 된다).
 * 문구가 아직 정해지지 않은 종류는 템플릿이 {@code {content}} 라서 {@code params} 에
 * {@code content} 키로 완성 문구를 넘기면 된다.
 *
 * <pre>
 * events.publishEvent(new NotificationRequestedEvent(
 *         userId, NotificationType.ACCOUNT_RECONNECT,
 *         Map.of("bankName", "국민은행"),
 *         "/asset/accounts/9/reconnect"));
 * </pre>
 *
 * @param userId      받을 사용자
 * @param type        알림 종류 (제목·문구 템플릿을 함께 들고 있다)
 * @param params      문구 템플릿의 자리표시자 값
 * @param deepLinkUrl 알림을 눌렀을 때 갈 화면. 경로는 발행하는 도메인이 안다
 */
public record NotificationRequestedEvent(
        Long userId,
        NotificationType type,
        Map<String, String> params,
        String deepLinkUrl) {
}

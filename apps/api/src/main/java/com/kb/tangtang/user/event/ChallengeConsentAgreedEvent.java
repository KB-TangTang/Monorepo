package com.kb.tangtang.user.event;

import java.time.LocalDateTime;

/** 챌린지 참여 동의가 비활성 상태에서 활성 상태로 변경된 사건. */
public record ChallengeConsentAgreedEvent(Long userId, LocalDateTime agreedAt) {
}

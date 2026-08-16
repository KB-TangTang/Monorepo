package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 그룹 상세의 「오늘의 소비 상태」 카드 한 장 (이슈 #169).
 *
 * <p>응답에는 <b>나를 뺀</b> 참여자만 담긴다. 내 몫은 상단 먹지 카드가 따로 쓴다.
 *
 * <p>{@code dailyAmount} 라는 이름이지만 기간평가 그룹에서는 기간 전체 합계다.
 * 화면 문구도 「오늘의 소비 상태」 ↔ 「멤버 누적 상태」로 갈린다
 * ({@code GroupDetailMemberTable.vue}). 필드를 둘로 나누면 화면이 매번 둘 중 하나를
 * 골라야 해서 한 이름으로 뒀다.
 */
@Getter
@Builder
public class GroupDailyMemberDto {

    private Long userId;
    private String nickname;
    /** 저장된 키가 아니라 조립된 URL 이다. 프로필을 안 올렸으면 NULL. */
    private String profileImageUrl;

    /** 평가 주기에 맞춘 소비액. 일일평가는 오늘 하루치, 기간평가는 기간 합계. */
    private BigDecimal dailyAmount;

    /**
     * 한도 대비 사용률(%). 100 을 넘을 수 있다 — 화면이 막대만 100% 에서 자른다.
     * 한도가 0원인 무지출 챌린지는 한 푼이라도 쓰면 100 이다.
     */
    private int usagePercent;

    /** 한도를 넘겼는지. JSON 키는 {@code exceeded} 다. */
    private boolean exceeded;

    /**
     * 이 참여자가 지금 재판 중이면 그 상태({@code DEFENSE_WAIT} · {@code VOTING}), 아니면 NULL.
     * 카드의 「재판중」 배지가 이 값으로 켜진다.
     */
    private String trialStatus;
}

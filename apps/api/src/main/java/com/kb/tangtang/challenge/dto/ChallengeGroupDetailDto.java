package com.kb.tangtang.challenge.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 그룹 챌린지 상세 화면 한 벌 (이슈 #169).
 *
 * <p>목록 카드가 쓰는 {@link ChallengeGroupDto} 에 상세 화면만 필요한 것을 얹었다.
 * 필드를 복사하지 않고 {@code @JsonUnwrapped} 로 <b>평평하게 펼쳐</b> 내보낸다 —
 * 프론트가 응답을 {@code {...dto}} 로 펼쳐 쓰기 때문에 {@code challenge.groupName} 처럼
 * 한 겹 접히면 화면 전체가 빈칸이 된다. 필드를 28개 베껴 두면 목록 쪽에 필드가 하나 늘 때마다
 * 여기도 같이 고쳐야 하고, 빠뜨려도 컴파일이 통과한다.
 *
 * <p><b>채우지 않는 것</b> — 화면이 이미 NULL 을 견디도록 만들어져 있다.
 * <ul>
 *   <li>{@code settleTime} · {@code memoAuthor} · {@code memoDate} — 근거 컬럼이 없다</li>
 *   <li>채팅 미리보기 — 이슈 #174</li>
 *   <li>{@code savingsAmount}(종료 화면) — 절약액 산식이 정해지지 않았다. BACKLOG 등록</li>
 * </ul>
 */
@Getter
@Builder
public class ChallengeGroupDetailDto {

    /** 목록·미리보기와 같은 그룹 정보. JSON 에서는 한 겹 없이 평평하게 펼쳐진다. */
    @JsonUnwrapped
    private ChallengeGroupDto challenge;

    /* ── 나 (상단 먹지 카드) ──────────────────────────── */
    /** 평가 주기에 맞춘 내 소비액. 일일평가는 오늘 하루치, 기간평가는 기간 합계. */
    private BigDecimal myDailyAmount;
    /** 한도 대비 내 사용률(%). 100 을 넘을 수 있다. */
    private int myUsagePercent;
    /** 남은 금액. <b>초과했으면 음수</b>다 — 0 으로 깎으면 얼마나 넘겼는지 화면이 알 수 없다. */
    private BigDecimal myRemainingAmount;

    /* ── 재판 · 참여자 ────────────────────────────────── */
    /** 진행 중인 기소. 오래된 순(= 마감이 급한 순). 없으면 빈 배열이다. */
    private List<GroupIndictmentDto> indictments;

    /** 소비 상태 카드. <b>나는 빠져 있다</b> — 내 몫은 위의 {@code my*} 세 개다. */
    private List<GroupDailyMemberDto> dailyMembers;

    /* ── 종료 화면 (이슈 #173) — CLOSED 전에는 NULL ────── */
    /**
     * 최종 결과 순 참여자. {@code final_rank} 오름차순 — <b>확정값을 읽기만 한다</b>(재계산 금지).
     * CLOSED 전에는 {@code final_*} 컬럼이 NULL 이라 만들 수 없어 NULL 로 둔다.
     */
    private List<FinalMember> finalMembers;

    /** 확정 재판 전적. 명예 법정 진입 링크의 숫자. CLOSED 전에는 NULL. */
    private TrialStats trialStats;

    /** 종료 화면 포디움·랭킹 테이블 한 줄. 확정 배치(#172)가 채운 값 그대로다. */
    @Getter
    @Builder
    public static class FinalMember {
        private Long userId;
        private String nickname;
        /** 없으면 NULL. 이니셜·색은 프론트가 닉네임에서 파생한다. */
        private String profileImageUrl;
        private Integer finalRank;
        /** SURVIVED / ELIMINATED. */
        private String finalOutcome;
        /** 종료 시점 남은 목숨. */
        private Integer livesCount;
    }

    /** 확정(GUILTY·INNOCENT) 재판 집계. 진행 중 재판은 세지 않는다. */
    @Getter
    @Builder
    public static class TrialStats {
        private int totalTrials;
        private int guiltyCount;
        private int innocentCount;
    }
}

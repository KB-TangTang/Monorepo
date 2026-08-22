package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 명예 법정(생존자 랭킹) 화면 한 벌 (이슈 #173). 한 번의 호출로 화면 전부를 그린다.
 *
 * <p>탈락 여부의 판단 근거는 서버가 명시적으로 내리는 {@code finalOutcome} 다.
 * 진행 중(ACTIVE)의 목숨 0 은 「탈락 위기」일 뿐 탈락이 아니다 — 그날 밤 결산·재판 결과에 따라
 * 살아날 수 있다. 「탈락」 표시는 {@code status = CLOSED} 이고 {@code finalOutcome = ELIMINATED}
 * 일 때만이며, 그 파생은 프론트 뷰모델이 한다.
 *
 * <p>아바타 색·이니셜은 내리지 않는다 — 프론트 {@code UserAvatar} 가 닉네임에서 파생한다(팀 관례).
 */
@Getter
@Builder
public class GroupRankingDto {

    /** {@code DAILY}(일일평가) / {@code PERIOD}(기간평가). 화면 종류가 갈린다. */
    private String evalType;

    /** 그룹 상태. {@code CLOSED} 면 순위가 확정값이고, 그 외에는 지금 시점의 스냅샷이다. */
    private String status;

    /** 제한 금액. 기간평가 화면의 사용률 게이지 분모다. */
    private Integer limitAmount;

    /** 시작 목숨 수. 일일평가 화면이 닳은 목숨 아이콘을 그릴 때 분모로 쓴다. */
    private int maxLives;

    /** 리워드·벌칙 자유 메모. 없으면 NULL 이고 화면이 카드 자체를 숨긴다. */
    private String memo;

    /**
     * 마지막으로 결산이 끝난 날짜. 일일평가 화면의 「{날짜} 결산 반영」 부제용.
     *
     * <p>진행 중에는 어제까지 중 결산 행이 있는 마지막 날(오늘은 재집계 중이라 제외),
     * 종료 후에는 {@code end_date} 다. 결산 행이 아직 없으면 NULL — 화면이 날짜 부분을 생략한다.
     */
    private LocalDate lastSettlementDate;

    /** 참여자 전원. 순위 오름차순 (동률은 공동 순위 1, 1, 3). */
    private List<Member> members;

    /** 랭킹 한 줄. */
    @Getter
    @Builder
    public static class Member {

        /** 순위. 진행 중에는 매 조회 계산값, 종료 후에는 {@code final_rank} 저장값이다. */
        private Integer rank;

        private Long userId;

        private String nickname;

        /** 프로필 이미지 URL. 이미지가 없으면 NULL — 화면이 이니셜 아바타로 대체한다. */
        private String profileImageUrl;

        /** 내 줄인지. 화면이 「나」 배지와 강조 테두리를 그린다. */
        private boolean mine;

        /** 남은 목숨. 일일평가 화면 전용이다. */
        private Integer livesCount;

        /** 누적 소비액(표시용, 무죄 감액 반영). 종료 후에도 확정 부담금과 별개로 내려간다. */
        private BigDecimal totalConsumption;

        /** {@code SURVIVED}/{@code ELIMINATED}. <b>종료(CLOSED) 전에는 NULL 이다.</b> */
        private String finalOutcome;

        /** 최종 실제 부담금. 종료(CLOSED) 전에는 NULL 이다. */
        private BigDecimal finalChargeAmount;
    }
}

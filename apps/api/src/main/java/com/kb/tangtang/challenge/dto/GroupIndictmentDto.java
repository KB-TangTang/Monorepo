package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 진행 중인 재판 카드 한 장 (이슈 #169 · #432).
 *
 * <p>두 화면이 같은 모양을 쓴다 — 그룹 상세의 재판 캐러셀과 지방법원 홈의 「재판 현황」.
 * 진행 중인 기소({@code DEFENSE_WAIT} · {@code VOTING})만 담는다. 확정된 기소는
 * 카드가 아니라 전적으로 간다.
 *
 * <p><b>카드 종류는 서버가 정하지 않는다.</b> 프론트가 {@code mine} · {@code status} ·
 * {@code defended} · {@code myVote} 네 값을 조합해 아래 <b>6가지</b>를 만든다
 * ({@code apps/web/src/utils/groupTrial.js}). 판정을 서버로 올리면 상태가 하나 늘 때마다
 * war 를 다시 올려야 한다.
 *
 * <pre>
 *   피고  상태           조건            내 입장
 *   ────────────────────────────────────────────────
 *   나    DEFENSE_WAIT  !defended       변론 필요   ← 할 일
 *   나    DEFENSE_WAIT  defended        변론 제출
 *   나    VOTING        —               심판받는 중
 *   남    DEFENSE_WAIT  —               변론 대기
 *   남    VOTING        myVote == null  투표 필요   ← 할 일
 *   남    VOTING        myVote != null  투표 완료
 * </pre>
 *
 * <p>마감 두 개를 <b>둘 다</b> 내려보낸다. 화면이 {@code status} 로 골라 쓴다 —
 * 상태별로 한 필드만 채우면 상태 전이 배치가 도는 순간 화면에서 마감이 잠깐 사라진다.
 */
@Getter
@Builder
public class GroupIndictmentDto {

    /** 기소 ID. 변론·투표 화면으로 넘어갈 때 쓰는 키다. */
    private Long id;

    /* ── 소속 그룹 (이슈 #432) ────────────────────────── */
    /**
     * 그룹 상세에서는 화면이 이미 아는 값이라 쓰지 않는다. 지방법원 홈은 여러 그룹의 재판을
     * 한 목록에 섞어 내리므로, 카드 제목의 카테고리·한도를 화면이 들고 있는 참여 그룹 목록과
     * 이 ID 로 조인해 채운다. (그 두 값을 서버가 또 내려보내지 않는 이유다)
     */
    private Long groupId;

    private String groupName;

    /* ── 피고 ─────────────────────────────────────────── */
    private Long userId;
    private String nickname;
    /** 저장된 키가 아니라 조립된 URL 이다. 프로필을 안 올렸으면 NULL — 화면이 이니셜을 그린다. */
    private String profileImageUrl;

    /** {@code DEFENSE_WAIT} · {@code VOTING}. */
    private String status;

    /**
     * 위반한 날짜. 기소 생성일이 아니다 — 심야 거래를 다음 날 배치가 잡아 하루 어긋난다.
     * 「8월 5일 결산」의 그 날짜다.
     */
    private LocalDate settlementDate;

    /** 한도 초과액. */
    private BigDecimal exceededAmount;

    /* ── 로그인 사용자 기준 ───────────────────────────── */
    /** 내가 피고인지. JSON 키는 {@code mine} 이다(Lombok 이 {@code isMine()} 을 만든다). */
    private boolean mine;
    /** 피고가 변론을 냈는지. 카드가 「변론 제출」로 바뀌는 조건이다. */
    private boolean defended;
    /** 내가 던진 표({@code GUILTY} · {@code INNOCENT}). 아직 안 던졌으면 NULL. */
    private String myVote;

    private int voteCount;
    /** 참여자 - 피고 1명. */
    private int totalVoters;

    /* ── 마감 (저장값이 아니라 계산값) ────────────────── */
    private LocalDateTime defenseDeadline;
    private LocalDateTime voteDeadline;
}

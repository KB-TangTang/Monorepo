package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배심원의 표 한 장 ({@code tbl_vote}, 이슈 #171).
 *
 * <p><b>PK 가 {@code (group_id, user_id, indictment_id)} 복합이다.</b> {@code groupId} 를
 * 빠뜨리면 INSERT 가 NOT NULL 로 죽는다 — 기소만 알면 그룹은 정해지지만 컬럼은 따로 있다.
 * 그 PK 자체가 <b>중복 투표의 마지막 방어선</b>이기도 하다(서비스가 먼저 막는다).
 *
 * <p>수정·삭제 경로가 없다. 표를 바꿀 수 있으면 개표 직전 눈치싸움이 생긴다(이슈 #171 결정).
 */
@Getter
@Setter
public class Vote {

    private Long groupId;

    /** 투표자. 피고 본인일 수 없다. */
    private Long userId;

    private Long indictmentId;

    /** {@link Verdict} 의 이름. */
    private String verdict;

    /** 익명 한줄 코멘트. 선택 입력이라 없으면 NULL 이다(빈 문자열로 넣지 않는다). */
    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

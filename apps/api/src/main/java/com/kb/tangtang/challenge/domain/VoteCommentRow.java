package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 판결 상세에 붙는 익명 코멘트 한 줄 (이슈 #171).
 *
 * <p><b>투표자를 특정할 수 있는 컬럼을 넣지 않는다.</b> {@code user_id} 를 담아 두면 DTO 로
 * 옮기다 실수로 새어 나가고, 그러면 「누가 나에게 유죄를 줬는지」 가 그룹에 공개된다.
 * 조회 SQL 도 {@code tbl_user} 를 조인하지 않는다.
 */
@Getter
@Setter
public class VoteCommentRow {

    private String comment;

    private LocalDateTime createdAt;
}

package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 판결 상세의 익명 한줄 코멘트 (이슈 #171).
 *
 * <p><b>투표자를 특정할 수 있는 필드를 절대 넣지 않는다.</b> 닉네임은 물론 {@code userId} 도
 * 안 된다 — 그룹은 인원이 적어 id 하나만 있어도 누구인지 바로 안다.
 *
 * <p>{@code createdAt} 은 표시 순서를 위한 것이지 투표자 식별용이 아니다. 그룹 안에서
 * 「누가 언제 투표했는지」 를 보여주는 화면이 없으므로 시각만으로는 특정되지 않는다.
 *
 * <p>개표가 끝난 재판에서만 내려간다. 투표 중에는 목록 자체가 NULL 이다 —
 * 문장에서 유무죄가 읽혀 표 분포가 새어 나가기 때문이다.
 */
@Getter
@Builder
public class TrialVoteCommentDto {

    /** 최대 40자. 비어 있는 코멘트는 목록에 넣지 않는다. */
    private String comment;

    private LocalDateTime createdAt;
}

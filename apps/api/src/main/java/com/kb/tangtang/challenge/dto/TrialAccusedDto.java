package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 재판의 피고 (이슈 #170).
 *
 * <p>{@link GroupTrialDetailDto} 안에 한 겹 접어 둔다. 재판 상세는 <b>피고 본인</b>도 보고
 * <b>투표자</b>도 보는데(이슈 #171), 화면이 "누가 피고인지" 를 한 덩어리로 읽는 편이
 * {@code accusedUserId} · {@code accusedNickname} … 처럼 접두를 붙여 늘어놓는 것보다 짧다.
 */
@Getter
@Builder
public class TrialAccusedDto {

    private Long userId;

    private String nickname;

    /** 표시용 URL. 키가 없으면 NULL 이고 화면이 기본 이미지를 쓴다. */
    private String profileImageUrl;

    /** 내가 피고인지. 화면이 변론 버튼과 투표 버튼을 이 값으로 가른다. */
    private boolean mine;
}

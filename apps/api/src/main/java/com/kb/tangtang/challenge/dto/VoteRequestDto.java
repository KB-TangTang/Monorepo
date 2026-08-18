package com.kb.tangtang.challenge.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 투표 요청 본문 (이슈 #171).
 *
 * <p>{@code verdict} 검증은 여기서 하지 않는다. enum 으로 받으면 오타 하나에
 * Jackson 이 400 을 내면서 우리 오류 코드({@code INVALID_VERDICT}) 대신 파싱 메시지를 내보낸다 —
 * 문자열로 받아 서비스가 판단한다.
 */
@Getter
@Setter
public class VoteRequestDto {

    /** {@code GUILTY} · {@code INNOCENT}. */
    private String verdict;

    /** 익명 한줄 코멘트. 선택 입력이고 최대 40자다. */
    private String comment;
}

package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 생성 직후 응답. 프론트가 초대 화면으로 넘어가려면 groupId 와 초대 코드가 필요하다.
 */
@Getter
@Builder
public class ChallengeGroupCreatedDto {

    private Long groupId;
    private String inviteCode;
}

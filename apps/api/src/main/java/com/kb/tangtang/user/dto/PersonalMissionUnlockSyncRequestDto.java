package com.kb.tangtang.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** 현재 맞춤 미션 데이터 충족 여부를 서버 상태와 동기화한다. */
@Getter
@NoArgsConstructor
public class PersonalMissionUnlockSyncRequestDto {

    private Boolean enoughData;

    public PersonalMissionUnlockSyncRequestDto(Boolean enoughData) {
        this.enoughData = enoughData;
    }
}


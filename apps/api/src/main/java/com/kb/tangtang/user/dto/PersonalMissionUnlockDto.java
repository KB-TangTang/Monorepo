package com.kb.tangtang.user.dto;

import com.kb.tangtang.user.domain.PersonalMissionUnlockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 맞춤 미션 개시 안내 상태 응답. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalMissionUnlockDto {

    private PersonalMissionUnlockStatus status;
    private boolean showUnlock;

    public static PersonalMissionUnlockDto from(PersonalMissionUnlockStatus status) {
        return PersonalMissionUnlockDto.builder()
                .status(status)
                .showUnlock(status == PersonalMissionUnlockStatus.PENDING)
                .build();
    }
}


package com.kb.tangtang.mission.listener;

import com.kb.tangtang.mission.service.DailyMissionAssignmentService;
import com.kb.tangtang.user.event.ChallengeConsentAgreedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChallengeConsentAgreedListenerTest {

    @Test
    void assignsMissionForAgreedUserInConfiguredZone() {
        DailyMissionAssignmentService service = mock(DailyMissionAssignmentService.class);
        ChallengeConsentAgreedListener listener = new ChallengeConsentAgreedListener(service, "Asia/Seoul");

        listener.onAgreed(new ChallengeConsentAgreedEvent(7L, LocalDateTime.now()));

        verify(service).assign(eq(7L), any());
    }
}

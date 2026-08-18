package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TossAuthSchedulerTest {

    @Test
    @DisplayName("갱신이 필요하면 발급받은 토큰으로 TossTokenHolder를 갱신한다")
    void updatesHolderWithFetchedTokenWhenNeeded() {
        TossAuthClient tossAuthClient = mock(TossAuthClient.class);
        TossTokenHolder tokenHolder = mock(TossTokenHolder.class);
        when(tokenHolder.needsRefresh(any(Duration.class))).thenReturn(true);
        when(tossAuthClient.fetchToken()).thenReturn(
                TossAccessToken.builder().accessToken("tok-1").expiresInSeconds(86_400).build());

        new TossAuthScheduler(tossAuthClient, tokenHolder).refresh();

        verify(tokenHolder).update("tok-1", 86_400);
    }

    @Test
    @DisplayName("토큰이 아직 유효하면(갱신 불필요) 토스를 부르지 않는다")
    void skipsWhenTokenStillFresh() {
        TossAuthClient tossAuthClient = mock(TossAuthClient.class);
        TossTokenHolder tokenHolder = mock(TossTokenHolder.class);
        when(tokenHolder.needsRefresh(any(Duration.class))).thenReturn(false);

        new TossAuthScheduler(tossAuthClient, tokenHolder).refresh();

        verifyNoInteractions(tossAuthClient);
        verify(tokenHolder, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("토큰 발급이 실패해도 예외를 삼키고 홀더를 갱신하지 않는다")
    void swallowsFailureWithoutUpdatingHolder() {
        TossAuthClient tossAuthClient = mock(TossAuthClient.class);
        TossTokenHolder tokenHolder = mock(TossTokenHolder.class);
        when(tokenHolder.needsRefresh(any(Duration.class))).thenReturn(true);
        when(tossAuthClient.fetchToken())
                .thenThrow(new BusinessException("EXTERNAL_API_UNAVAILABLE", "토스 인증 서버에 연결하지 못했어요."));

        new TossAuthScheduler(tossAuthClient, tokenHolder).refresh();

        verify(tokenHolder, never()).update(any(), anyLong());
    }
}

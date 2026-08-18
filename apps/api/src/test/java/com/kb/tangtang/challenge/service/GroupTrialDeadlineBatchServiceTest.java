package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 변론 마감 배치 (이슈 #170).
 *
 * <p>마감 판정은 SQL 이 {@code created_at} 과 {@code NOW()} 를 비교해서 한다
 * ({@code ChallengeMapperXmlTest} 가 그 모양을 지킨다). 여기서는 <b>서비스가 프로퍼티를
 * 그대로 넘기는지</b>와 <b>행별 루프를 돌지 않는지</b>만 본다.
 */
@ExtendWith(MockitoExtension.class)
class GroupTrialDeadlineBatchServiceTest {

    private static final int DEFENSE_HOURS = 6;

    @Mock private IndictmentMapper indictmentMapper;

    private GroupTrialDeadlineBatchService service() {
        return new GroupTrialDeadlineBatchService(indictmentMapper, DEFENSE_HOURS);
    }

    /**
     * 시간 값을 SQL 에 상수로 박지 않고 프로퍼티에서 받은 값을 그대로 넘겨야 한다.
     * {@code challenge.trial.defense-hours} 를 바꿨는데 배치만 옛 값으로 돌면
     * 변론 창과 마감 판정이 어긋나 <b>낼 수 있었던 변론이 마감된다.</b>
     */
    @Test
    @DisplayName("마감 시간은 프로퍼티 값을 그대로 SQL 에 넘긴다")
    void passesConfiguredDefenseHours() {
        when(indictmentMapper.moveExpiredDefensesToVoting(DEFENSE_HOURS)).thenReturn(2);

        assertEquals(2, service().closeExpiredDefenses());

        verify(indictmentMapper).moveExpiredDefensesToVoting(DEFENSE_HOURS);
    }

    /**
     * <b>UPDATE 는 한 번만 나간다.</b> 대상을 SELECT 해서 행마다 UPDATE 하면 두 틱이 겹칠 때
     * 같은 행을 두 번 잡고, 조회와 갱신 사이에 사용자가 변론을 내면 그 변론이 무시된 채
     * 상태가 옮겨진다.
     */
    @Test
    @DisplayName("대상을 조회해 행별로 돌지 않고 UPDATE 한 번으로 끝낸다")
    void movesEverythingInOneStatement() {
        when(indictmentMapper.moveExpiredDefensesToVoting(DEFENSE_HOURS)).thenReturn(5);

        service().closeExpiredDefenses();

        verify(indictmentMapper, times(1)).moveExpiredDefensesToVoting(DEFENSE_HOURS);
    }

    /**
     * 두 번 돌려도 결과가 같다 — 멱등의 근거는 SQL 의 {@code WHERE status = 'DEFENSE_WAIT'} 다.
     * 두 번째 실행에서 0행이 나오는 것이 정상이며 예외가 되어서는 안 된다(스케줄러가 5분마다 돈다).
     */
    @Test
    @DisplayName("옮길 대상이 없으면 0을 돌려주고 조용히 끝낸다")
    void returnsZeroWhenNothingExpired() {
        when(indictmentMapper.moveExpiredDefensesToVoting(DEFENSE_HOURS)).thenReturn(0);

        assertEquals(0, service().closeExpiredDefenses());
    }
}

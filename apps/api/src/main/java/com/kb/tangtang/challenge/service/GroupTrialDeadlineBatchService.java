package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 변론 마감 처리 (이슈 #170 배치). 마감이 지난 {@code DEFENSE_WAIT} 기소를 {@code VOTING} 으로 넘긴다.
 *
 * <p>스케줄러와 나눠 둔다 — DEV 수동 트리거({@code /api/dev/batches/group-trial-deadline})와 단위
 * 테스트가 이 서비스를 직접 부른다. 스케줄러에 로직이 있으면 그 경로들이 다른 코드를 타게 된다.
 * ({@link ChallengeGroupStatusScheduler} 와 같은 구조)
 *
 * <p><b>UPDATE 한 문장이다.</b> 기소를 하나씩 읽어 옮기지 않는다 — 마감 판정에 필요한 정보가
 * {@code created_at} 하나라 SQL 이 다 할 수 있고, 건별 루프는 같은 기소를 두 틱이 동시에
 * 처리할 여지를 만든다.
 *
 * <p>알림·채팅 메시지를 발행하지 않는다. 「변론 없이 마감」은 사용자가 아무것도 하지 않아 생긴
 * 일이라 알릴 내용이 없고, 투표 요청은 배심원의 「오늘의 할 일」이 상태를 보고 이미 잡아낸다.
 */
@Service
@Log4j2
public class GroupTrialDeadlineBatchService {

    private final IndictmentMapper indictmentMapper;

    /** 기소 후 변론을 낼 수 있는 시간. 마감 계산의 유일한 근거다. */
    private final int defenseHours;

    public GroupTrialDeadlineBatchService(IndictmentMapper indictmentMapper,
                                          @Value("${challenge.trial.defense-hours}") int defenseHours) {
        this.indictmentMapper = indictmentMapper;
        this.defenseHours = defenseHours;
    }

    /**
     * 마감이 지난 변론 대기를 투표 대기로 넘긴다.
     *
     * <p><b>몇 번을 돌려도 같은 상태가 된다.</b> {@code WHERE status = 'DEFENSE_WAIT'} 가
     * 이미 넘어간 건을 걸러 낸다.
     *
     * @return 넘긴 건수
     */
    @Transactional
    public int closeExpiredDefenses() {
        int moved = indictmentMapper.moveExpiredDefensesToVoting(defenseHours);
        if (moved > 0) {
            log.info("변론 마감 → 투표 전이 count={} defenseHours={}", moved, defenseHours);
        }
        return moved;
    }
}

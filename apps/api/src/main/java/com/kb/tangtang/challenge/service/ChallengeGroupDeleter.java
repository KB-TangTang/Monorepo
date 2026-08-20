package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * 모집 중인 그룹 챌린지를 지우는 절차 (이슈 #352).
 *
 * <p>지우는 주체가 둘이다 — <b>상태 전이 배치의 미성립 처리</b>
 * ({@link ChallengeGroupStatusTransitionService})와 <b>방장의 삭제</b>
 * ({@link ChallengeGroupService#deleteGroup}). 절차가 같은데 양쪽에 복붙하면
 * 한쪽만 고쳐지는 순간 갈라지므로 여기 한 곳에 둔다. 알림 문구만 각자 다르다.
 *
 * <p><b>알림을 여기서 발행하지 않는 이유</b>가 그것이다. 미성립은 방장에게 「성립되지
 * 않았어요」를, 방장 삭제는 남은 참여자에게 「방장이 없앴어요」를 보낸다. 대상도 문구도
 * 다르므로 「지웠는가」만 알려 주고 알림은 부르는 쪽이 만든다.
 */
@Component
@Log4j2
public class ChallengeGroupDeleter {

    private final ChallengeGroupMapper challengeGroupMapper;
    private final ChatMessageStore chatMessageStore;

    public ChallengeGroupDeleter(ChallengeGroupMapper challengeGroupMapper,
                                 ChatMessageStore chatMessageStore) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.chatMessageStore = chatMessageStore;
    }

    /**
     * 그룹이 아직 RECRUITING 일 때만 지운다. 참여자·기소·투표·결산 행은 FK CASCADE 가 함께 지운다.
     *
     * <p>상태를 조회해 확인하지 않고 DELETE 의 WHERE 에 넣는 이유는, 조회와 삭제 사이에 다른
     * 요청이 끼어들어 그룹이 ACTIVE 로 시작돼 버릴 수 있어서다. 그 틈에 지우면 <b>정상 시작한
     * 그룹이 통째로 사라진다.</b> 바뀐 행 수가 곧 「내가 지웠는가」의 답이다.
     *
     * @return 이번 호출이 실제로 지웠으면 {@code true}. 이미 RECRUITING 이 아니었으면 {@code false}
     */
    public boolean deleteIfRecruiting(long groupId) {
        int deleted = challengeGroupMapper.deleteIfCurrent(
                groupId, ChallengeGroupStatus.RECRUITING.name());
        if (deleted == 0) {
            return false;
        }
        // TTL 만 믿지 않는다. 종료 = 즉시 차단 + 즉시 삭제(이슈 #174)
        // Redis 장애로 삭제가 실패해도 그룹 삭제라는 본업은 이미 끝났다 — 조용히 삼키지 않고 로그만 남긴다.
        try {
            chatMessageStore.deleteRoom(groupId);
        } catch (Exception e) {
            log.error("채팅방 삭제 실패 groupId={} — TTL 로 뒤늦게 정리된다", groupId, e);
        }
        return true;
    }
}

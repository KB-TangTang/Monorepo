package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 채팅방 입장·구독·발행 전에 거치는 검증.
 *
 * <p>REST 와 STOMP 양쪽이 이 클래스를 쓴다. 검증 규칙이 두 벌이 되면 한쪽만 뚫린다.
 *
 * <p>CLOSED 만 막고 JUDGING 은 허용한다 — 재판 중이 대화가 가장 활발한 구간이고,
 * 막으면 판결 시스템 메시지를 아무도 보지 못한다(DECISIONS.md 2026-08-15).
 *
 * <p>참여자 판정은 <b>Redis 캐시 우선, 미스면 DB</b> 다. 캐시는 DB 의 사본일 뿐이라
 * 캐시에 없다는 이유로 거부하지 않는다 — 그러면 캐시 갱신이 한 번 실패한 사용자가 키 만료
 * 시점까지 채팅에서 영구 차단된다.
 *
 * <p>{@code ChallengeGroup.status} 는 {@code String} 컬럼이라 {@link ChallengeGroupStatus}
 * 와 직접 비교할 수 없다. {@code name()} 으로 변환해 비교한다 — enum 타입으로 바꾸는
 * 리팩터링은 그룹챌린지 전체에 영향을 주므로 이 작업 범위 밖이다.
 */
@Service
public class ChatRoomAccessService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomAccessService.class);

    private final ChallengeGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final ChatMessageStore store;

    public ChatRoomAccessService(ChallengeGroupMapper groupMapper,
                                 GroupMemberMapper memberMapper,
                                 ChatMessageStore store) {
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.store = store;
    }

    @Transactional(readOnly = true)
    public ChallengeGroup verifyCanEnter(long groupId, long userId) {
        ChallengeGroup group = groupMapper.findById(groupId);
        if (group == null) {
            throw new BusinessException("NOT_FOUND", "존재하지 않는 챌린지예요.");
        }
        if (ChallengeGroupStatus.CLOSED.name().equals(group.getStatus())) {
            throw new BusinessException("CHAT_ROOM_CLOSED", "종료된 챌린지의 대화는 볼 수 없어요.");
        }

        // 캐시 히트면 DB 를 보지 않는다 — 채팅은 메시지마다 이 검증을 거치므로 여기서 DB 를 타면 안 된다.
        if (store.memberIds(groupId).contains(userId)) {
            return group;
        }

        /*
         * 캐시에 없다고 곧바로 거부하면 안 된다. ChallengeGroupService.join 의 cacheMembers 는
         * 실패를 삼키고 로그만 남기므로(참여 자체를 되돌리지 않는 옳은 설계다), Redis 가 잠깐
         * 흔들리면 캐시는 "비어 있지 않지만 이 사용자만 빠진" 상태로 굳는다. 그러면 members 키가
         * 만료될 때까지(최대 end_date+2일) 그 사용자는 채팅에서 영구 차단된다.
         * 최종 판단은 DB 다. 확인되면 캐시를 다시 데워 다음 요청부터는 캐시 경로로 돌아온다.
         */
        Set<Long> fromDb = new HashSet<>(memberMapper.findUserIdsByGroupId(groupId));
        if (!fromDb.contains(userId)) {
            throw new BusinessException("CHAT_NOT_MEMBER", "이 챌린지의 참여자가 아니에요.");
        }
        warmCache(groupId, fromDb, group);
        return group;
    }

    /** 캐시 갱신 실패가 입장을 막지 않게 한다 — 캐시는 어디까지나 DB 의 사본이다 */
    private void warmCache(long groupId, Set<Long> memberIds, ChallengeGroup group) {
        if (group.getEndDate() == null) {
            return;
        }
        try {
            store.cacheMembers(groupId, memberIds, group.getEndDate());
        } catch (Exception e) {
            log.warn("참여자 캐시 갱신 실패 groupId={}", groupId, e);
        }
    }

    /**
     * Redis 캐시 우선. 비어 있으면 DB 에서 읽어 캐시를 데운다.
     * 캐시를 데우려면 TTL 기준(endDate)이 필요해 그룹을 한 번 더 조회한다 — 그룹이 없거나
     * endDate 가 없으면 캐시는 건드리지 않고 DB 조회 결과만 반환한다.
     */
    @Transactional(readOnly = true)
    public Set<Long> memberIdsOf(long groupId) {
        Set<Long> cached = store.memberIds(groupId);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<Long> fromDb = memberMapper.findUserIdsByGroupId(groupId);
        Set<Long> ids = new HashSet<>(fromDb);
        ChallengeGroup group = groupMapper.findById(groupId);
        if (group != null && group.getEndDate() != null) {
            store.cacheMembers(groupId, ids, group.getEndDate());
        }
        return ids;
    }
}

package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.common.exception.BusinessException;
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
 * <p>{@code ChallengeGroup.status} 는 {@code String} 컬럼이라 {@link ChallengeGroupStatus}
 * 와 직접 비교할 수 없다. {@code name()} 으로 변환해 비교한다 — enum 타입으로 바꾸는
 * 리팩터링은 그룹챌린지 전체에 영향을 주므로 이 작업 범위 밖이다.
 */
@Service
public class ChatRoomAccessService {

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
        if (!memberIdsOf(groupId).contains(userId)) {
            throw new BusinessException("CHAT_NOT_MEMBER", "이 챌린지의 참여자가 아니에요.");
        }
        return group;
    }

    /** Redis 캐시 우선. 비어 있으면 DB 에서 읽어 캐시를 데운다 */
    @Transactional(readOnly = true)
    public Set<Long> memberIdsOf(long groupId) {
        Set<Long> cached = store.memberIds(groupId);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<Long> fromDb = memberMapper.findUserIdsByGroupId(groupId);
        Set<Long> ids = new HashSet<>(fromDb);
        store.cacheMembers(groupId, ids);
        return ids;
    }
}

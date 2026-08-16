package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomAccessServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long USER_ID = 3L;

    @Mock private ChallengeGroupMapper groupMapper;
    @Mock private GroupMemberMapper memberMapper;
    @Mock private ChatMessageStore store;

    @InjectMocks private ChatRoomAccessService service;

    // 판정 1: ChallengeGroup 은 setter 가 없다(id 제외). 빌더로 만든다.
    // 판정 2: status 필드는 String 이다. ChallengeGroupStatus.name() 을 넣는다.
    private ChallengeGroup groupWith(ChallengeGroupStatus status) {
        return ChallengeGroup.builder()
                .id(GROUP_ID)
                .groupName("절약단")
                .status(status.name())
                .endDate(LocalDate.of(2026, 8, 20))
                .build();
    }

    @Test
    @DisplayName("JUDGING 상태에서도 입장할 수 있다")
    void allowsEnterWhileJudging() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.JUDGING));
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(USER_ID));

        assertEquals(ChallengeGroupStatus.JUDGING.name(), service.verifyCanEnter(GROUP_ID, USER_ID).getStatus());
    }

    @Test
    @DisplayName("CLOSED 면 입장을 막는다")
    void rejectsClosedRoom() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.CLOSED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyCanEnter(GROUP_ID, USER_ID));

        assertEquals("CHAT_ROOM_CLOSED", ex.getCode());
    }

    @Test
    @DisplayName("캐시에도 DB 에도 없으면 막는다")
    void rejectsNonMember() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.ACTIVE));
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(99L));
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(99L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyCanEnter(GROUP_ID, USER_ID));

        assertEquals("CHAT_NOT_MEMBER", ex.getCode());
    }

    /*
     * 리뷰 I2: ChallengeGroupService.join 의 cacheMembers 는 실패를 삼키고 로그만 남긴다
     * (참여 자체를 되돌리지 않는 옳은 설계다). 그래서 Redis 가 잠깐 흔들리면 캐시는
     * "비어 있지 않은데 이 사용자만 빠진" 상태로 굳고, 예전 코드는 그것만 보고 곧바로
     * CHAT_NOT_MEMBER 를 던져 members 키가 만료될 때까지(최대 end_date+2일) 영구 차단했다.
     */
    @Test
    @DisplayName("캐시가 낡아 빠져 있어도 DB 에 있으면 입장시키고 캐시를 다시 데운다")
    void fallsBackToDatabaseWhenCacheIsStale() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.ACTIVE));
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(99L));
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(99L, USER_ID));

        assertEquals(ChallengeGroupStatus.ACTIVE.name(),
                service.verifyCanEnter(GROUP_ID, USER_ID).getStatus());

        verify(store).cacheMembers(GROUP_ID, Set.of(99L, USER_ID), LocalDate.of(2026, 8, 20));
    }

    @Test
    @DisplayName("캐시 갱신이 실패해도 입장 자체는 막지 않는다")
    void staleCacheFallbackSurvivesCacheWriteFailure() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.ACTIVE));
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(99L));
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(99L, USER_ID));
        org.mockito.Mockito.doThrow(new RuntimeException("Redis 장애"))
                .when(store).cacheMembers(anyLong(), anySet(), any());

        assertEquals(ChallengeGroupStatus.ACTIVE.name(),
                service.verifyCanEnter(GROUP_ID, USER_ID).getStatus());
    }

    @Test
    @DisplayName("입장 검증은 캐시 히트면 DB 를 건드리지 않는다")
    void verifyCanEnterUsesCacheWhenPresent() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.ACTIVE));
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(USER_ID, 99L));

        service.verifyCanEnter(GROUP_ID, USER_ID);

        verify(memberMapper, never()).findUserIdsByGroupId(GROUP_ID);
        verify(store, never()).cacheMembers(anyLong(), anySet(), any());
    }

    @Test
    @DisplayName("없는 그룹이면 막는다")
    void rejectsUnknownGroup() {
        when(groupMapper.findById(GROUP_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyCanEnter(GROUP_ID, USER_ID));

        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("참여자 캐시가 비어 있으면 DB 에서 읽어 캐시를 채우고 그룹의 endDate 로 TTL 을 넘긴다")
    void fallsBackToDatabaseAndWarmsCache() {
        LocalDate endDate = LocalDate.of(2026, 8, 20);
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of());
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(3L, 9L));
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWith(ChallengeGroupStatus.ACTIVE));

        Set<Long> ids = service.memberIdsOf(GROUP_ID);

        assertEquals(Set.of(3L, 9L), ids);
        verify(store).cacheMembers(GROUP_ID, Set.of(3L, 9L), endDate);
    }

    @Test
    @DisplayName("참여자 캐시가 있으면 DB 를 건드리지 않는다")
    void usesCacheWhenPresent() {
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of(3L, 9L));

        service.memberIdsOf(GROUP_ID);

        verify(memberMapper, never()).findUserIdsByGroupId(GROUP_ID);
        verify(groupMapper, never()).findById(GROUP_ID);
    }

    @Test
    @DisplayName("캐시 미스인데 그룹이 없으면 캐시를 데우지 않고 DB 결과만 반환한다")
    void missWithUnknownGroupSkipsCaching() {
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of());
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(3L, 9L));
        when(groupMapper.findById(GROUP_ID)).thenReturn(null);

        Set<Long> ids = service.memberIdsOf(GROUP_ID);

        assertEquals(Set.of(3L, 9L), ids);
        verify(store, never()).cacheMembers(anyLong(), anySet(), any());
    }

    @Test
    @DisplayName("캐시 미스인데 그룹의 endDate 가 없으면 캐시를 데우지 않고 DB 결과만 반환한다")
    void missWithNullEndDateSkipsCaching() {
        when(store.memberIds(GROUP_ID)).thenReturn(Set.of());
        when(memberMapper.findUserIdsByGroupId(GROUP_ID)).thenReturn(List.of(3L, 9L));
        ChallengeGroup groupWithoutEndDate = ChallengeGroup.builder()
                .id(GROUP_ID)
                .groupName("절약단")
                .status(ChallengeGroupStatus.ACTIVE.name())
                .endDate(null)
                .build();
        when(groupMapper.findById(GROUP_ID)).thenReturn(groupWithoutEndDate);

        Set<Long> ids = service.memberIdsOf(GROUP_ID);

        assertEquals(Set.of(3L, 9L), ids);
        verify(store, never()).cacheMembers(anyLong(), anySet(), any());
    }
}

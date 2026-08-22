package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.GroupRankingRow;
import com.kb.tangtang.challenge.dto.GroupRankingDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 명예 법정 랭킹 조회 (이슈 #173).
 *
 * <p>순위 계산·정렬은 SQL 이 하고 {@code ChallengeMapperXmlTest} 가 모양을 지킨다.
 * 여기서는 서비스의 몫만 본다 — <b>상태별 조회 경로 분기(종료 후 재계산 금지)</b>,
 * 참여자 검증, {@code mine} 플래그, URL 변환, 마지막 결산일.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupRankingServiceTest {

    private static final long USER_ID = 7L;
    private static final long GROUP_ID = 3L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);
    private static final LocalDate START = LocalDate.of(2026, 8, 12);
    private static final LocalDate END = LocalDate.of(2026, 8, 18);

    @Mock private ChallengeGroupMapper challengeGroupMapper;
    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private ImageStorage imageStorage;

    private GroupRankingService service() {
        return new GroupRankingService(challengeGroupMapper, resultMapper, imageStorage,
                Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        ZoneId.systemDefault()));
    }

    private void givenGroup(String status, String evalType) {
        when(challengeGroupMapper.findById(GROUP_ID)).thenReturn(ChallengeGroup.builder()
                .id(GROUP_ID).status(status).evalType(evalType)
                .limitAmount(10_000).memo("꼴찌가 커피 쏘기")
                .startDate(START).endDate(END)
                .build());
    }

    private GroupRankingRow row(int rank, long userId) {
        GroupRankingRow row = new GroupRankingRow();
        row.setRank(rank);
        row.setUserId(userId);
        row.setNickname("절약왕" + userId);
        row.setLivesCount(3);
        row.setTotalConsumption(new BigDecimal("5000"));
        return row;
    }

    @Test
    @DisplayName("없는 그룹은 GROUP_NOT_FOUND 로 거절된다")
    void rejectsUnknownGroup() {
        when(challengeGroupMapper.findById(GROUP_ID)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().findRanking(USER_ID, GROUP_ID));

        assertEquals("GROUP_NOT_FOUND", e.getCode());
    }

    /** 랭킹에는 피고 닉네임·소비액이 전부 실린다. 참여자가 아니면 그룹 ID 만으로 열리면 안 된다. */
    @Test
    @DisplayName("참여자가 아니면 GROUP_NOT_MEMBER 로 거절된다")
    void rejectsNonMember() {
        givenGroup("ACTIVE", "DAILY");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY"))
                .thenReturn(List.of(row(1, 9L), row(2, 10L)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().findRanking(USER_ID, GROUP_ID));

        assertEquals("GROUP_NOT_MEMBER", e.getCode());
    }

    /**
     * 종료 후 재계산 금지 (#172 계약). CLOSED 에서 Active 경로가 불리면 확정 순위 대신
     * 지금 시점의 재계산 순위가 내려가 이미 보여 준 순위가 뒤집힌다.
     */
    @Test
    @DisplayName("CLOSED 그룹은 확정값 조회만 하고 순위 재계산 경로를 부르지 않는다")
    void closedGroupNeverRecalculates() {
        givenGroup("CLOSED", "DAILY");
        GroupRankingRow mine = row(1, USER_ID);
        mine.setFinalOutcome("SURVIVED");
        mine.setFinalChargeAmount(new BigDecimal("5000"));
        when(resultMapper.findRankingClosed(GROUP_ID)).thenReturn(List.of(mine));

        GroupRankingDto ranking = service().findRanking(USER_ID, GROUP_ID);

        verify(resultMapper, never()).findRankingActive(anyLong(), anyString());
        assertEquals("SURVIVED", ranking.getMembers().get(0).getFinalOutcome());
    }

    /** 진행 중의 목숨 0 은 「탈락 위기」다. finalOutcome 이 NULL 로 내려가야 화면이 탈락으로 그리지 않는다. */
    @Test
    @DisplayName("진행 중 그룹은 순위를 계산하고 finalOutcome 은 NULL 이다")
    void activeGroupUsesLiveRanking() {
        givenGroup("ACTIVE", "DAILY");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY")).thenReturn(List.of(row(1, USER_ID)));

        GroupRankingDto ranking = service().findRanking(USER_ID, GROUP_ID);

        verify(resultMapper, never()).findRankingClosed(anyLong());
        assertNull(ranking.getMembers().get(0).getFinalOutcome());
        assertNull(ranking.getMembers().get(0).getFinalChargeAmount());
    }

    @Test
    @DisplayName("내 줄에만 mine 이 켜진다")
    void flagsMyRow() {
        givenGroup("ACTIVE", "DAILY");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY"))
                .thenReturn(List.of(row(1, 9L), row(2, USER_ID)));

        List<GroupRankingDto.Member> members = service().findRanking(USER_ID, GROUP_ID).getMembers();

        assertFalse(members.get(0).isMine());
        assertTrue(members.get(1).isMine());
    }

    /** 응답에는 키가 아니라 URL 이 나간다. 키가 그대로 나가면 화면의 <img> 가 전부 깨진다. */
    @Test
    @DisplayName("프로필 이미지 키는 URL 로 변환돼 내려간다")
    void convertsProfileImageKeyToUrl() {
        givenGroup("ACTIVE", "DAILY");
        GroupRankingRow withImage = row(1, USER_ID);
        withImage.setProfileImageKey("profile/7.png");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY")).thenReturn(List.of(withImage));
        when(imageStorage.urlOf("profile/7.png")).thenReturn("https://img.example/profile/7.png");

        GroupRankingDto ranking = service().findRanking(USER_ID, GROUP_ID);

        assertEquals("https://img.example/profile/7.png",
                ranking.getMembers().get(0).getProfileImageUrl());
    }

    /** 진행 중에는 오늘 행이 5분 배치로 재집계되는 중이라 어제까지의 결산일을 쓴다. */
    @Test
    @DisplayName("진행 중 그룹의 마지막 결산일은 오늘을 제외하고 조회한다")
    void activeGroupReadsLastSettlementDate() {
        givenGroup("ACTIVE", "DAILY");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY")).thenReturn(List.of(row(1, USER_ID)));
        when(resultMapper.findLastSettlementDate(GROUP_ID, TODAY)).thenReturn(TODAY.minusDays(1));

        assertEquals(TODAY.minusDays(1), service().findRanking(USER_ID, GROUP_ID).getLastSettlementDate());
    }

    /** 종료 후 마지막 결산일은 end_date 자체다. 조회로 얻으면 종료 뒤 행이 지워졌을 때 NULL 이 된다. */
    @Test
    @DisplayName("CLOSED 그룹의 마지막 결산일은 end_date 고 결산 조회를 하지 않는다")
    void closedGroupUsesEndDateAsSettlementDate() {
        givenGroup("CLOSED", "DAILY");
        GroupRankingRow mine = row(1, USER_ID);
        mine.setFinalOutcome("SURVIVED");
        when(resultMapper.findRankingClosed(GROUP_ID)).thenReturn(List.of(mine));

        GroupRankingDto ranking = service().findRanking(USER_ID, GROUP_ID);

        assertEquals(END, ranking.getLastSettlementDate());
        verify(resultMapper, never()).findLastSettlementDate(anyLong(), any());
    }

    /** 목숨 아이콘의 분모다. 참여 INSERT 와 같은 규칙(일일평가 = 기간 일수, 기간평가 = 1)이어야 한다. */
    @Test
    @DisplayName("maxLives 는 일일평가 = 기간 일수, 기간평가 = 1 이다")
    void maxLivesFollowsEvalType() {
        givenGroup("ACTIVE", "DAILY");
        when(resultMapper.findRankingActive(GROUP_ID, "DAILY")).thenReturn(List.of(row(1, USER_ID)));
        assertEquals(7, service().findRanking(USER_ID, GROUP_ID).getMaxLives());

        givenGroup("ACTIVE", "PERIOD");
        when(resultMapper.findRankingActive(GROUP_ID, "PERIOD")).thenReturn(List.of(row(1, USER_ID)));
        assertEquals(1, service().findRanking(USER_ID, GROUP_ID).getMaxLives());
    }
}

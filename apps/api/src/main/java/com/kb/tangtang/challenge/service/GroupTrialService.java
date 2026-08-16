package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 재판 진입로 — 홈 「오늘의 할 일」과 그룹 상세의 재판 카드 (이슈 #169).
 *
 * <p>{@link ChallengeGroupService} 에 넣지 않았다. 그쪽은 그룹 자체(생성·초대·참여)를 다루는데
 * 여기는 {@code tbl_indictment} 만 읽는다. 한 클래스에 묶으면 그룹을 고치는 사람과 재판을 고치는
 * 사람이 같은 파일에서 충돌한다 — 6명이 병렬로 작업하는 프로젝트다.
 *
 * <p><b>마감 시각은 여기서 계산한다.</b> {@code tbl_indictment} 에 마감 컬럼이 없어
 * {@code created_at} 에 {@code challenge.trial.*} 시간을 더한다. 계산을 매퍼(SQL)로 내리지 않은
 * 이유는 프로퍼티를 SQL 에 넣으려면 {@code ${}} 가 필요해서다 — 팀 규칙상 금지다.
 */
@Service
public class GroupTrialService {

    private final IndictmentMapper indictmentMapper;
    private final ImageStorage imageStorage;

    /** 기소 후 변론을 낼 수 있는 시간. */
    private final int defenseHours;

    /** 변론 마감 후 투표를 받는 시간. */
    private final int voteHours;

    @Autowired
    public GroupTrialService(IndictmentMapper indictmentMapper,
                             ImageStorage imageStorage,
                             @Value("${challenge.trial.defense-hours}") int defenseHours,
                             @Value("${challenge.trial.vote-hours}") int voteHours) {
        this.indictmentMapper = indictmentMapper;
        this.imageStorage = imageStorage;
        this.defenseHours = defenseHours;
        this.voteHours = voteHours;
    }

    /**
     * 내가 지금 처리해야 하는 재판 전부. 마감 임박순.
     *
     * <p>변론 대기와 투표 대기를 각각 조회해 합친다. 한 쿼리로 UNION 하면 두 조회의 조인·조건이
     * 서로 다른데도 컬럼 수를 억지로 맞춰야 해서, 한쪽만 고칠 때 다른 쪽이 조용히 깨진다.
     * 한 사람이 동시에 지고 있는 재판은 많아야 수십 건이라 쿼리 두 번이 부담되지 않는다.
     *
     * <p><b>마감이 지난 건도 그대로 내려간다.</b> 걸러내는 주체는 상태 전이 배치(#170) 하나여야 한다 —
     * 근거는 {@link IndictmentMapper#findDefenseTodos} 주석에 있다.
     */
    @Transactional(readOnly = true)
    public List<MyTrialDto> findMyTrials(long userId) {
        List<MyTrialDto> trials = new ArrayList<>();

        for (TrialTodoRow row : indictmentMapper.findDefenseTodos(userId)) {
            trials.add(MyTrialDto.builder()
                    .indictmentId(row.getIndictmentId())
                    .type("accuse")
                    .challengeId(row.getChallengeId())
                    .challengeName(row.getChallengeName())
                    .amount(row.getAmount())
                    .deadline(row.getCreatedAt().plusHours(defenseHours))
                    .build());
        }

        for (TrialTodoRow row : indictmentMapper.findVoteTodos(userId)) {
            trials.add(MyTrialDto.builder()
                    .indictmentId(row.getIndictmentId())
                    .type("vote")
                    .challengeId(row.getChallengeId())
                    .challengeName(row.getChallengeName())
                    .defendantNickname(row.getDefendantNickname())
                    .voteCount(row.getVoteCount())
                    .totalVoters(row.getTotalVoters())
                    .deadline(row.getCreatedAt().plusHours(defenseHours + voteHours))
                    .build());
        }

        /*
         * 정렬은 서버가 끝낸다. 화면이 카드(상위 2건)와 바텀시트(전체) 두 곳에서 같은 배열을 쓰는데,
         * 프론트가 각자 정렬하면 카드에 보이던 건이 시트에서는 세 번째로 밀리는 일이 생긴다.
         * 같은 마감이면 id 순 — 순서가 매 요청 흔들리면 화면이 이유 없이 재정렬된다.
         */
        trials.sort(Comparator.comparing(MyTrialDto::getDeadline)
                .thenComparing(MyTrialDto::getIndictmentId));
        return trials;
    }

    /**
     * 그룹 상세의 재판 카드. 진행 중인 기소만, 오래된 순(= 마감이 급한 순).
     *
     * <p><b>참여자 검증을 하지 않는다.</b> 호출부({@link ChallengeGroupDetailService})가 먼저
     * 그룹 접근 권한을 확인한 뒤 부른다. 여기서 한 번 더 확인하려면 참여자 목록을 다시 읽어야 하고,
     * 검증이 두 군데로 갈리면 나중에 규칙이 바뀔 때 한쪽만 고쳐진다.
     *
     * <p>마감 두 개를 모두 채운다 — 화면이 {@code status} 로 골라 쓰는 이유는
     * {@link GroupIndictmentDto} 주석에 있다.
     */
    @Transactional(readOnly = true)
    public List<GroupIndictmentDto> findGroupIndictments(long userId, long groupId) {
        List<GroupIndictmentDto> cards = new ArrayList<>();
        for (GroupIndictmentRow row : indictmentMapper.findOpenByGroupId(groupId, userId)) {
            cards.add(GroupIndictmentDto.builder()
                    .id(row.getId())
                    .userId(row.getUserId())
                    .nickname(row.getNickname())
                    .profileImageUrl(imageStorage.urlOf(row.getProfileImageKey()))
                    .status(row.getStatus())
                    .settlementDate(row.getChallengeDate())
                    .exceededAmount(row.getExceededAmount())
                    .mine(row.getUserId() != null && row.getUserId() == userId)
                    .defended(row.isDefended())
                    .myVote(row.getMyVerdict())
                    .voteCount(row.getVoteCount())
                    .totalVoters(row.getTotalVoters())
                    .defenseDeadline(row.getCreatedAt().plusHours(defenseHours))
                    .voteDeadline(row.getCreatedAt().plusHours(defenseHours + voteHours))
                    .build());
        }
        return cards;
    }
}

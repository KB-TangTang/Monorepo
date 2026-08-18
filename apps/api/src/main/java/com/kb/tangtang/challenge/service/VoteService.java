package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.Verdict;
import com.kb.tangtang.challenge.domain.Vote;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.challenge.mapper.VoteMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 소비 재판 투표 (이슈 #171). 표를 <b>쓰는 쪽</b>이다.
 *
 * <p>읽는 쪽({@code myVerdict} · 표수 · 코멘트)은 이미 {@link GroupTrialService} 에 있다 —
 * 재판 상세 한 문장이 그 값을 함께 내려준다. 여기에 조회를 또 만들면 같은 집계가 두 벌이 된다.
 *
 * <p>{@link DefenseService} 와 같은 구조다 — {@code @Autowired} 생성자가
 * {@code Clock.systemDefaultZone()} 을 채워 package-private 생성자에 위임하고, 테스트는
 * 후자에 {@code Clock.fixed} 를 준다. 마감 검증이 실행 시각에 따라 통과·실패를 오가면 안 된다.
 *
 * <p><b>개표는 여기서 하지 않는다.</b> 마지막 한 표가 들어와도 상태를 바꾸지 않는다 —
 * 투표 마감까지 기다렸다 한 번에 세는 것이 이슈 #172 의 몫이다. 표가 다 모였다고 즉시
 * 확정하면 마감 전에 결과가 나와 「남은 사람은 투표할 이유가 없는」 재판이 된다.
 */
@Service
public class VoteService {

    /** 화면(`VoteVerdictView.vue` 의 MAX_COMMENT)과 같은 값. 클라이언트만 믿지 않는다. */
    private static final int COMMENT_MAX_LENGTH = 40;

    private final IndictmentMapper indictmentMapper;
    private final VoteMapper voteMapper;
    private final Clock clock;

    /** 기소 후 변론을 낼 수 있는 시간. 투표 마감을 계산하는 데 쓴다. */
    private final int defenseHours;

    /** 변론 마감 후 투표를 받는 시간. */
    private final int voteHours;

    @Autowired
    public VoteService(IndictmentMapper indictmentMapper,
                       VoteMapper voteMapper,
                       @Value("${challenge.trial.defense-hours}") int defenseHours,
                       @Value("${challenge.trial.vote-hours}") int voteHours) {
        this(indictmentMapper, voteMapper, defenseHours, voteHours, Clock.systemDefaultZone());
    }

    VoteService(IndictmentMapper indictmentMapper,
                VoteMapper voteMapper,
                int defenseHours,
                int voteHours,
                Clock clock) {
        this.indictmentMapper = indictmentMapper;
        this.voteMapper = voteMapper;
        this.defenseHours = defenseHours;
        this.voteHours = voteHours;
        this.clock = clock;
    }

    /**
     * 표 한 장을 던진다.
     *
     * <p>검증 순서는 {@link DefenseService#registerDefense} 와 같다 — <b>권한 → 상태 → 중복 →
     * 입력값</b>. 없는 재판을 「권한 없음」으로 알려 주면 남의 그룹에 그 기소가 있다는 사실이
     * 새어 나가므로 조회 실패는 언제나 {@code TRIAL_NOT_FOUND} 다.
     *
     * <p><b>수정은 없다.</b> 이미 던졌으면 {@code VOTE_ALREADY_EXISTS} 다. 바꿀 수 있으면
     * 개표 직전 눈치싸움이 생긴다(이슈 #171 결정).
     *
     * <p><b>이벤트를 발행하지 않는다.</b> 투표 한 건마다 그룹에 알리면 누가 언제 투표했는지가
     * 채팅에 남아 익명이 깨진다. 개표 알림은 이슈 #172 의 {@code VerdictConfirmed} 몫이다.
     *
     * @param comment NULL 이거나 공백만이면 코멘트 없이 저장한다(선택 입력)
     */
    @Transactional
    public void vote(long userId, long indictmentId, String verdict, String comment) {
        GroupTrialDetailRow row = indictmentMapper.findTrialDetail(indictmentId, userId);
        if (row == null) {
            throw new BusinessException("TRIAL_NOT_FOUND", "재판을 찾을 수 없습니다.");
        }
        if (!IndictmentStatus.VOTING.name().equals(row.getStatus())) {
            throw new BusinessException("VOTE_NOT_ALLOWED", "투표 기간이 아닙니다.");
        }

        /*
         * 마감은 저장값이 아니라 created_at + (변론 시간 + 투표 시간) 계산값이다.
         * 개표 배치(#172)가 아직 없고, 있더라도 주기 사이에 「상태는 VOTING 인데 마감은 지난」
         * 구간이 남는다 — 상태만 보면 그 구간에 표가 들어온다.
         */
        if (row.getCreatedAt().plusHours(defenseHours + voteHours)
                .isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException("VOTE_NOT_ALLOWED", "투표 마감 시간이 지났습니다.");
        }
        if (row.getUserId() != null && row.getUserId() == userId) {
            throw new BusinessException("CANNOT_VOTE_OWN_TRIAL", "자신의 재판에는 투표할 수 없습니다.");
        }
        if (voteMapper.existsByIndictmentIdAndUserId(indictmentId, userId)) {
            throw new BusinessException("VOTE_ALREADY_EXISTS", "이미 투표했습니다.");
        }
        if (!Verdict.isValid(verdict)) {
            throw new BusinessException("INVALID_VERDICT", "유죄 또는 무죄를 선택해주세요.");
        }

        String text = comment == null ? "" : comment.trim();
        if (text.length() > COMMENT_MAX_LENGTH) {
            throw new BusinessException("COMMENT_TOO_LONG",
                    COMMENT_MAX_LENGTH + "자 이내로 입력해주세요.");
        }

        Vote vote = new Vote();
        /* group_id 는 PK 의 일부다. 빠뜨리면 NOT NULL 로 INSERT 가 죽는다 */
        vote.setGroupId(row.getGroupId());
        vote.setUserId(userId);
        vote.setIndictmentId(indictmentId);
        vote.setVerdict(verdict);
        /* 빈 문자열로 넣지 않는다 — 코멘트 목록 조회가 「있는데 비어 있는」 행을 걸러야 한다 */
        vote.setComment(text.isEmpty() ? null : text);
        voteMapper.insert(vote);
    }
}

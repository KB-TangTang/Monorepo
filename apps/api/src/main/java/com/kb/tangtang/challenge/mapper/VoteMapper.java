package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.Vote;
import com.kb.tangtang.challenge.domain.VoteCommentRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code tbl_vote} 접근 (이슈 #171).
 *
 * <p>INSERT 와 조회만 있다. 표는 한 번 던지면 끝이라 UPDATE·DELETE 경로를 만들지 않는다 —
 * 바꿀 수 있으면 개표 직전에 눈치싸움이 생긴다.
 *
 * <p>투표 현황({@code myVerdict} · {@code voteCount} · 유무죄 표수)은 여기가 아니라
 * {@code IndictmentMapper.xml#findTrialDetail} 의 서브쿼리가 센다. 재판 상세 한 문장으로
 * 끝내야 화면이 두 번 왕복하지 않는다.
 *
 * <p>{@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface VoteMapper {

    /**
     * 표 저장.
     *
     * <p>{@code groupId} 를 반드시 채운다 — PK 가 {@code (group_id, user_id, indictment_id)} 복합이다.
     *
     * <p>같은 조합을 두 번 넣으면 PK 로 {@code DuplicateKeyException} 이 난다. 호출부가 미리
     * {@link #existsByIndictmentIdAndUserId} 로 걸러 사용자에게 오류 코드를 주지만,
     * 동시 요청이 들어오면 이 PK 가 마지막 방어선이다.
     */
    int insert(Vote vote);

    /** 내가 이미 이 재판에 투표했는지. 중복 투표를 오류 코드로 알리는 데 쓴다. */
    boolean existsByIndictmentIdAndUserId(@Param("indictmentId") Long indictmentId,
                                          @Param("userId") Long userId);

    /**
     * 판결 상세의 익명 코멘트 목록 (오래된 순).
     *
     * <p><b>개표가 끝난 재판에만 쓴다.</b> 투표 중에 코멘트를 내려보내면 문장에서 유무죄가
     * 그대로 읽혀 표 분포가 새어 나가고, 편승 투표가 생긴다. 노출 판단은
     * {@code GroupTrialService} 한 곳에서만 한다.
     *
     * <p>코멘트는 선택 입력이라 <b>비어 있는 표는 결과에 넣지 않는다.</b> 빈 줄이 목록에 섞이면
     * 화면에 이유 없는 여백이 생긴다.
     */
    List<VoteCommentRow> findCommentsByIndictmentId(@Param("indictmentId") Long indictmentId);
}

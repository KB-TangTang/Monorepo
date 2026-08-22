package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code tbl_group_member} 접근.
 */
@Mapper
public interface GroupMemberMapper {

    int insertMember(GroupMember member);

    /**
     * 여러 그룹의 참여자를 한 번에 읽는다. 목록 조회의 N+1 을 피하기 위한 것이다.
     * 상세·참여 판정도 이 메서드에 그룹 하나만 넘겨 쓴다 — 정원·목숨·"내가 참여자인가"가
     * 모두 참여자 목록 하나로 결정되므로 조회를 나눌 이유가 없다.
     */
    List<GroupMember> findByGroupIds(@Param("groupIds") List<Long> groupIds);

    /** 채팅 참여자 캐시를 채울 때 쓴다. 캐시가 살아 있으면 호출되지 않는다 */
    List<Long> findUserIdsByGroupId(@Param("groupId") long groupId);

    /**
     * 참여 정원의 <b>최종</b> 판정용 참여자 목록 (이슈 #354).
     * {@code ChallengeGroupMapper.lockGroupForJoin} 으로 그룹 행을 잠근 <b>뒤에</b> 부른다.
     *
     * <p><b>이것도 {@code FOR UPDATE} 여야 한다.</b> MySQL 기본 격리수준(REPEATABLE READ)에서
     * 일반 SELECT 는 트랜잭션의 첫 읽기 시점 스냅샷을 본다. 그룹 행을 잠근 뒤
     * {@link #findByGroupIds} 로 다시 세면 <b>그 사이 커밋된 참여가 스냅샷에 없어</b>
     * 잠금을 걸고도 예전 숫자를 그대로 읽는다 — 정원 방어가 통째로 무력해진다.
     * 잠금 읽기는 스냅샷이 아니라 최신 커밋본을 본다.
     *
     * <p>{@code tbl_user} 를 조인하지 않는다. 조인하면 참여자들의 사용자 행까지 잠긴다.
     * 채워지는 것은 {@code groupId} · {@code userId} 뿐이다 — 판정에 쓰는 것이
     * 「이미 참여자인가」와 「몇 명인가」뿐이라 그 외에는 필요 없다.
     * <b>표시용으로 쓰지 말 것</b>({@code nickname} · {@code livesCount} 가 NULL 이다).
     */
    List<GroupMember> findByGroupIdForUpdate(@Param("groupId") long groupId);

    /**
     * 유죄 확정 — 목숨 1개 차감 (이슈 #172).
     *
     * <p><b>음수 방어를 SQL 이 한다</b>({@code AND lives_count > 0}). 자바에서 읽고 빼서 쓰면
     * 읽기와 쓰기 사이에 다른 재판이 같은 사람의 목숨을 깎을 수 있다 — 하루에 재판이 여러 건
     * 확정되는 그룹에서 실제로 겹친다.
     *
     * <p>「하루 1목숨」 상한을 세는 카운터는 두지 않는다. 기소가 {@code uk_ind_result} 로
     * 결과 행당 1건뿐이라 같은 날 같은 사람이 두 번 기소될 수 없다.
     *
     * @return 바꾼 행 수. 이미 0목숨이면 0 이다(정상. 탈락 여부는 종료 후 확정 배치가 정한다)
     */
    int decreaseLife(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 최종 결과 확정 — {@code final_*} 3개를 한 번에 적는다 (이슈 #172).
     *
     * <p><b>{@code final_outcome IS NULL} 조건이 write-once 장치다.</b> 이미 확정된 참여자는
     * 0 행이 바뀌고 값이 보존된다. 조건이 없으면 배치가 두 번 도는 사이 재판이 하나 더 확정됐을 때
     * 앞서 사용자에게 보여 준 순위·완주 여부가 조용히 뒤집힌다.
     *
     * <p>세 값을 나눠 쓰지 않는다. 등수만 들어가고 판정이 비는 중간 상태를 화면이 읽으면
     * 「완주인지 탈락인지 모르는 1위」가 뜬다.
     *
     * @param finalOutcome      {@code SURVIVED} · {@code ELIMINATED}
     *                          ({@code FinalOutcome} 의 이름 그대로. CHECK 제약이 검사한다)
     * @param finalRank         생존자는 소비액 오름차순, 탈락자는 전원 공동 최하위
     * @param finalChargeAmount 무죄 감액이 반영된 기간 전체 실효 소비액
     * @return 바꾼 행 수. 0 이면 이미 확정됐다는 뜻이다(정상. 배치 재실행)
     */
    int finalizeMember(@Param("groupId") Long groupId,
                       @Param("userId") Long userId,
                       @Param("finalOutcome") String finalOutcome,
                       @Param("finalRank") Integer finalRank,
                       @Param("finalChargeAmount") BigDecimal finalChargeAmount);
}

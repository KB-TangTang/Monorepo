package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.GroupTrialSummaryRow;
import com.kb.tangtang.challenge.domain.Indictment;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code tbl_indictment} 접근.
 *
 * {@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface IndictmentMapper {

    /**
     * 기소 생성. 채번된 id 는 {@code indictment.id} 에 채워진다.
     *
     * <p><b>{@code result_id} 가 이미 기소된 행이면 {@code DuplicateKeyException} 이 난다.</b>
     * {@code uk_ind_result} UNIQUE 때문이고, 배치에서는 <b>정상 경로</b>다 —
     * 5분마다 도는 배치가 같은 위반을 다시 잡거나 두 인스턴스가 동시에 INSERT 하면 발생한다.
     * 호출부는 이 예외를 잡아 알림 발행까지 통째로 건너뛴다. 예외를 잡지 않으면
     * 그룹 하나의 실패가 트랜잭션 전체를 되돌린다.
     * ({@code db/migration/20260815_add_indictment_result_unique.sql})
     *
     * <p>{@code status} 는 항상 {@code DEFENSE_WAIT} 이고
     * {@code result} · {@code verdict_method} · {@code ai_verdict_reason} 은 NULL 이다.
     * DB CHECK(ck_ind_result)가 이 조합만 허용한다.
     */
    int insertIndictment(Indictment indictment);

    /**
     * 내가 피고이고 아직 변론을 내지 않은 기소 (이슈 #169). 홈 「오늘의 할 일」의 {@code accuse} 줄.
     *
     * <p><b>마감이 지났는지는 여기서 거르지 않는다.</b> 마감은 {@code created_at} 에서 계산하는
     * 값이라 SQL 로 거를 수는 있지만, 그러면 상태를 넘기는 배치(#170)와 <b>판단 주체가 둘</b>이 된다.
     * 배치가 5분 늦게 돌면 할 일이 사라졌다가 투표 줄로 다시 나타난다. 진실은 {@code status} 하나다.
     * 마감 시각은 화면에 표시만 한다.
     *
     * <p>{@code idx_ind_user_status (user_id, status)} 를 그대로 탄다.
     */
    List<TrialTodoRow> findDefenseTodos(@Param("userId") Long userId);

    /**
     * 내가 참여 중인 그룹에서 투표가 열려 있는데 내가 아직 안 던진 기소 (이슈 #169).
     * 홈 「오늘의 할 일」의 {@code vote} 줄.
     *
     * <p>피고 본인은 제외한다 — 자기 재판에는 투표할 수 없다.
     */
    List<TrialTodoRow> findVoteTodos(@Param("userId") Long userId);

    /**
     * 그룹 상세의 재판 카드 목록 (이슈 #169). 아직 확정되지 않은 기소만.
     *
     * @param userId 보는 사람. 내 표({@code myVerdict})를 붙이는 데 쓴다
     */
    List<GroupIndictmentRow> findOpenByGroupId(@Param("groupId") Long groupId,
                                               @Param("userId") Long userId);

    /**
     * 목록 카드의 재판 배지를 한 번에 센다 (이슈 #169).
     *
     * <p>재판이 없는 그룹은 <b>행이 나오지 않는다.</b> 호출부가 기본값(0 · null)으로 채운다 —
     * 여기서 참여 그룹 전체를 LEFT JOIN 으로 만들어 내면 재판이 하나도 없는 평시에도
     * 항상 참여 그룹 수만큼 행이 나온다.
     *
     * @param groupIds 비어 있으면 호출하지 않는다. MyBatis {@code foreach} 가 빈 목록으로
     *                 {@code IN ()} 을 만들면 SQL 문법 오류가 난다
     */
    List<GroupTrialSummaryRow> findTrialSummaryByGroupIds(@Param("userId") Long userId,
                                                          @Param("groupIds") List<Long> groupIds);
}

package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
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

    /**
     * 재판 상세 한 벌 (이슈 #170). 기소 안내 · 변론 작성 · 투표(#171) · 판결 상세가 모두 이것을 쓴다.
     *
     * <p><b>권한 검사를 SQL 이 겸한다.</b> {@code tbl_group_member} 조인이 있어 그룹 사람이
     * 아니면 행이 나오지 않는다. 호출부는 NULL 을 {@code TRIAL_NOT_FOUND} 로 바꾼다 —
     * 권한 오류와 구분하면 남의 그룹에 그 기소가 있다는 사실이 새어 나간다.
     *
     * <p>피고 본인도 멤버라 같은 조인으로 통과한다. 「내가 피고인지」는 {@code userId} 비교로
     * 서비스가 판단한다.
     *
     * @param userId 보는 사람. 내 표({@code myVerdict})와 권한 검사에 쓴다
     * @return 없거나 볼 권한이 없으면 NULL
     */
    GroupTrialDetailRow findTrialDetail(@Param("indictmentId") Long indictmentId,
                                       @Param("userId") Long userId);

    /**
     * 변론 등록 후 투표 대기로 넘긴다 (이슈 #170).
     *
     * <p><b>{@code WHERE ... AND status = 'DEFENSE_WAIT'} 가 동시 요청 방어다.</b> 같은 사용자가
     * 제출 버튼을 두 번 눌러도 두 번째는 0행을 바꾼다. 서비스가 그 0 을 보고 되돌린다 —
     * 읽고 나서 쓰는 사이에 상태가 바뀌면 SELECT 검증만으로는 못 막는다.
     *
     * @return 바꾼 행 수. 0 이면 이미 남이(또는 배치가) 상태를 옮겼다
     */
    int moveToVoting(@Param("indictmentId") Long indictmentId);

    /**
     * 혐의 인정 — 변론 대기에서 유죄로 바로 확정한다 (이슈 #170).
     *
     * <p>{@code result = 1} 을 같이 쓰는 이유는 DB CHECK({@code ck_ind_result})가
     * {@code GUILTY} + {@code result = 1} 조합만 허용해서다. 상태만 바꾸면 INSERT 가 아니라
     * UPDATE 여도 제약에 걸린다.
     *
     * <p><b>목숨 차감·{@code verdict_deduction_amount} 기록은 하지 않는다.</b> 판결 확정 후처리는
     * 이슈 #172 담당이고, 여기서 같이 하면 투표 개표 경로와 후처리가 두 벌이 된다.
     *
     * @return 바꾼 행 수. 0 이면 이미 변론이 등록됐거나 마감 배치가 투표로 넘겼다
     */
    int confirmConfession(@Param("indictmentId") Long indictmentId);

    /**
     * 변론 마감이 지난 기소를 한꺼번에 투표로 넘긴다 (이슈 #170 배치).
     *
     * <p><b>{@code WHERE status = 'DEFENSE_WAIT'} 하나로 멱등이다.</b> 몇 번을 돌려도 같은 상태가
     * 되고, 이미 변론을 낸 건(=VOTING)이나 혐의를 인정한 건(=GUILTY)은 건드리지 않는다.
     *
     * <p>마감 시각을 컬럼으로 두지 않는 이유는 {@code db/schema.sql:478-482} 주석에 있다 —
     * {@code created_at} 에서 계산한다. 그 시간이 프로퍼티라서 SQL 에 상수로 박을 수 없어
     * 파라미터로 받는다({@code ${}} 금지).
     *
     * @param defenseHours {@code challenge.trial.defense-hours}. 호출부가 그대로 넘긴다
     * @return 넘긴 건수
     */
    int moveExpiredDefensesToVoting(@Param("defenseHours") int defenseHours);
}

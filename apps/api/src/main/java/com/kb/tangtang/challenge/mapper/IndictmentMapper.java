package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.GroupTrialSummaryRow;
import com.kb.tangtang.challenge.domain.Indictment;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
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
     * <p><b>마감이 지났는지는 SQL 이 거르지 않는다.</b> 마감 시간은 {@code challenge.trial.*}
     * 프로퍼티라 SQL 에 넣으려면 {@code ${}} 가 필요하다 — 팀 규칙상 금지다.
     * 거르는 일은 {@link com.kb.tangtang.challenge.service.GroupTrialService#findMyTrials} 가 한다.
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

    /**
     * 개표할 차례가 된 기소 (이슈 #172 배치).
     *
     * <p><b>마감이 지났거나, 투표할 사람이 전부 던졌거나</b> 둘 중 하나면 잡는다. 마감만 보면
     * 전원이 일찍 투표를 끝낸 재판도 30시간을 채워야 결과가 나온다 — 그 사이 화면은
     * 「투표 완료」인 채로 멈춰 있어 사용자는 서비스가 멈춘 줄 안다(팀 결정 2026-08-18).
     *
     * <p>분모는 {@code 참여자 수 - 1} 이다(피고는 자기 재판에 투표할 수 없다). 중도 이탈 기능이
     * 없어 분모가 줄지 않는 것을 전제로 한다 — 나가기가 생기면 이 조건부터 다시 본다.
     *
     * <p>{@code status = 'VOTING'} 이라 혐의 인정(GUILTY)·이미 확정된 건은 애초에 걸리지 않는다.
     * 변론 없이 마감된 재판도 그대로 투표에 올라와 있으므로 여기 잡힌다 — 변론 유무는 보지 않는다.
     *
     * <p>시간 값은 프로퍼티라 파라미터로 받는다({@code ${}} 금지).
     *
     * @param defenseHours {@code challenge.trial.defense-hours}
     * @param voteHours    {@code challenge.trial.vote-hours}. 투표 마감은 둘의 합이다
     */
    List<VerdictTallyRow> findVotingToTally(@Param("defenseHours") int defenseHours,
                                            @Param("voteHours") int voteHours);

    /**
     * 판결 확정 — {@code VOTING} → {@code GUILTY} / {@code INNOCENT} (이슈 #172).
     *
     * <p><b>{@code WHERE ... AND status = 'VOTING'} 이 멱등의 근거다.</b> 배치를 두 번 돌리거나
     * 두 인스턴스가 같은 틱을 잡아도 두 번째는 0행을 바꾼다. 호출부는 그 0 을 보고 목숨 차감·
     * 감액 반영·이벤트 발행을 통째로 건너뛴다 — 조건이 없으면 <b>목숨이 두 번 깎인다.</b>
     *
     * <p>{@code result} 를 함께 쓰는 이유는 {@code ck_ind_result} 가 상태와의 조합만 허용해서다.
     * {@code aiVerdictReason} 은 {@code AI_JUDGMENT} 가 아니면 반드시 NULL 이어야 한다
     * ({@code ck_ind_ai_reason}).
     *
     * @return 바꾼 행 수. 0 이면 이미 확정된 재판이다
     */
    int confirmVerdict(@Param("indictmentId") Long indictmentId,
                       @Param("status") String status,
                       @Param("result") int result,
                       @Param("verdictMethod") String verdictMethod,
                       @Param("aiVerdictReason") String aiVerdictReason);
}

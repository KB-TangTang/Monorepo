package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupChallengeDailyResult;
import com.kb.tangtang.challenge.domain.IndictmentTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code tbl_group_challenge_daily_result} 접근. 평가·기소 배치(#168)가 쓴다.
 *
 * {@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface GroupChallengeResultMapper {

    /**
     * 그룹 전원의 하루치 소비를 다시 집계해 UPSERT 한다. 한 번에 그룹 전체를 처리한다.
     *
     * <p><b>{@code verdict_deduction_amount} 는 절대 건드리지 않는다.</b> UPDATE 절에 넣는 순간
     * 무죄 판결로 인정된 감액이 5분마다 0 으로 초기화되고, 화면에는 아무 흔적도 남지 않는다.
     * ({@code db/migration/20260814_group_challenge_verdict_deduction.sql})
     *
     * <p>거래가 없는 참여자도 0원 행이 생긴다. 랭킹이 참여자 전원을 보여야 하기 때문이다.
     *
     * @param challengeDate 집계 대상 날짜. 호출부가 챌린지 기간 안인지 먼저 확인한다
     * @return 영향받은 행 수. ON DUPLICATE KEY UPDATE 라 삽입 1 · 갱신 2 로 세어져 의미가 없다
     */
    int upsertDailyResults(@Param("groupId") Long groupId,
                           @Param("challengeDate") LocalDate challengeDate);

    /**
     * 일일평가(DAILY) 기소 대상. 그 날 {@code effective_amount} 가 한도를 넘었고 아직 기소가 없는 참여자.
     *
     * <p>비교 대상이 {@code daily_amount} 가 아니라 {@code effective_amount} 인 것이 중요하다.
     * 무죄로 감액을 받은 참여자가 그 감액 덕에 한도 이하가 됐다면 다시 기소하지 않는다.
     *
     * <p>이미 기소된 행을 걸러 내는 것은 <b>성능을 위한 것이지 정합성 장치가 아니다.</b>
     * 조회와 INSERT 사이에 다른 인스턴스가 끼어들 수 있어, 중복을 실제로 막는 것은
     * {@code uk_ind_result} UNIQUE 다.
     */
    List<IndictmentTarget> findOverLimitDaily(@Param("groupId") Long groupId,
                                              @Param("challengeDate") LocalDate challengeDate);

    /**
     * 기간평가(PERIOD) 기소 대상. 기간 전체 {@code SUM(daily_amount)} 가 한도를 넘긴 참여자.
     *
     * <p><b>호출 시점을 호출부가 통제한다.</b> 챌린지가 끝나기 전에는 부르지 않는다
     * ({@code today > end_date} 일 때만). 기간 도중 합계가 한도를 넘었다고 기소하면
     * "기간 전체를 보고 판단한다" 는 기간평가의 정의가 무너진다.
     *
     * <p>기소는 {@code end_date} 행에 붙인다. 어느 날의 거래가 한도를 넘겼는지 특정할 수 없고,
     * 변론 화면도 이 날짜가 아니라 그룹의 {@code start_date ~ end_date} 로 거래를 조회한다.
     *
     * <p>{@code effective_amount} 가 아니라 {@code daily_amount} 를 더한다. 기간평가는 감액을
     * {@code tbl_defense.deduction_amount} 에만 저장하고 일별 행에 분배하지 않으므로
     * ({@code verdict_deduction_amount} 가 항상 0) 두 값이 같다. 의도를 드러내려고 원본을 쓴다.
     */
    List<IndictmentTarget> findOverLimitPeriod(@Param("groupId") Long groupId);

    /**
     * {@code verdict_deduction_amount > daily_amount} 인 이상 행.
     *
     * <p>{@code effective_amount} 가 {@code GREATEST(..., 0)} 이라 이 상태가 화면에서는
     * 그냥 0원으로 보인다. 환불·카테고리 재분류로 집계액이 줄면 실제로 생기는 상태이므로
     * 배치가 매 틱 확인해 로그로 남긴다. 마이그레이션이 배치에 요구한 사항이다.
     * ({@code db/migration/20260814_group_challenge_verdict_deduction.sql})
     *
     * <p>고치지는 않는다. 원본 두 컬럼이 그대로 남아 있어 언제든 되짚을 수 있고,
     * 배치가 판결 컬럼을 손대는 순간 위의 소유권 분리가 깨진다.
     */
    List<GroupChallengeDailyResult> findDeductionOverflow(@Param("groupId") Long groupId);
}

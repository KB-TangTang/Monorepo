package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code tbl_challenge_group} 접근.
 *
 * {@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface ChallengeGroupMapper {

    /** 생성. 채번된 id 는 {@code group.id} 에 채워진다. */
    int insertGroup(ChallengeGroup group);

    ChallengeGroup findById(@Param("groupId") Long groupId);

    ChallengeGroup findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 로그인 사용자가 참여 중인 그룹 목록.
     *
     * 화면의 「종료됨」 탭이 JUDGING·CLOSED 두 상태를 함께 보여줘야 해서 목록으로 받는다.
     *
     * @param statuses NULL 또는 빈 목록이면 전체
     */
    List<ChallengeGroup> findMyGroups(@Param("userId") Long userId,
                                      @Param("statuses") List<String> statuses);

    int countByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 시작일이 도래했는데 아직 넘겨받은 상태에 머물러 있는 그룹. 상태 전이 배치(이슈 #152)가 쓴다.
     *
     * {@code idx_cg_status_start (status, start_date)} 를 그대로 탄다 — 선행 컬럼이 상등 비교,
     * 후행 컬럼이 범위 비교라 인덱스 순서와 조건 모양이 맞는다.
     *
     * @param status 찾을 상태. 값을 밖에서 넘겨 {@code ChallengeGroupStatus} 를 유일한 출처로 둔다
     * @param today  배치 기준일. 시스템 시각을 SQL(CURDATE) 이 아니라 애플리케이션이 정한다 —
     *               그래야 테스트가 고정 Clock 으로 날짜를 바꿔 끼울 수 있다
     */
    List<ChallengeGroup> findGroupsToStart(@Param("status") String status,
                                           @Param("today") LocalDate today);

    /**
     * 기대한 상태일 때만 상태를 바꾼다(compare-and-set).
     *
     * <b>배치 멱등성의 근거가 이 반환값이다.</b> 두 번째 실행이나 동시에 도는 인스턴스는 0 을 받고,
     * 그때 알림 발행을 건너뛴다. 조회 후 무조건 UPDATE 하면 배치를 두 번 돌릴 때
     * 같은 알림이 두 번 나간다.
     *
     * @return 바꾼 행 수. 0 이면 이미 다른 쪽이 바꿨다는 뜻이다
     */
    int updateStatusIfCurrent(@Param("groupId") Long groupId,
                              @Param("fromStatus") String fromStatus,
                              @Param("toStatus") String toStatus);
}

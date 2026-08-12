package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}

package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}

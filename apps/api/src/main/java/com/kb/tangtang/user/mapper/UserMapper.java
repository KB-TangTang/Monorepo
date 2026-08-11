package com.kb.tangtang.user.mapper;

import com.kb.tangtang.user.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Mapper 는 필수다. RootConfig 가 @MapperScan(annotationClass = Mapper.class) 로
 * 제한하고 있어 붙이지 않으면 빈으로 등록되지 않는다.
 */
@Mapper
public interface UserMapper {

    UserDto findBySocialId(@Param("socialProvider") String socialProvider,
                           @Param("providerUserId") String providerUserId);

    /** 실행 후 user.getId() 에 생성된 PK 가 채워진다. */
    void insert(UserDto user);

    UserDto findById(@Param("id") Long id);

    /**
     * 실명 갱신 (간편인증 화면). 반환값은 실제로 바뀐 행 수다 —
     * 0 이면 그 사용자가 없다는 뜻이라 서비스가 404 로 바꾼다.
     */
    int updateName(@Param("id") Long id, @Param("name") String name);
}

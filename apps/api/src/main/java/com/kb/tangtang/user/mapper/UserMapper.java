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

    /**
     * 철회하지 않은 필수 동의 건수. 0 이면 아직 동의 절차를 밟지 않은 사용자다.
     * 동의 화면은 후속 이슈지만, needsConsent 를 가짜 값으로 두지 않기 위해 여기서 조회한다.
     */
    int countActiveConsents(@Param("userId") Long userId);
}

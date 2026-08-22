package com.kb.tangtang.user.mapper;

import com.kb.tangtang.user.dto.RefreshTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

    void insert(RefreshTokenDto token);

    /** 폐기 여부와 무관하게 조회한다. 재사용 감지를 하려면 폐기된 행도 보여야 한다. */
    RefreshTokenDto findByHash(@Param("tokenHash") String tokenHash);

    void revokeById(@Param("id") Long id);

    /** 탈취 의심 시 해당 사용자의 살아 있는 토큰을 전부 폐기한다. */
    void revokeAllByUserId(@Param("userId") Long userId);
}

package com.kb.tangtang.user.service;

import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈취 의심 시의 일괄 폐기만 담당한다.
 *
 * 왜 별도 클래스인가:
 * 폐기 직후 호출자가 BusinessException(RuntimeException)을 던지므로, 같은 트랜잭션에 있으면
 * Spring 기본 롤백 정책이 폐기를 되돌려 버린다. REQUIRES_NEW 로 독립 트랜잭션에서 커밋해야
 * 바깥이 롤백돼도 폐기가 살아남는다.
 *
 * RefreshTokenService 안의 메서드로 두면 self-invocation 이라 프록시를 타지 않아
 * @Transactional 이 통째로 무시된다. 반드시 다른 빈이어야 한다.
 */
@Service
public class RefreshTokenSecurityService {

    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenSecurityService(RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(Long userId) {
        refreshTokenMapper.revokeAllByUserId(userId);
    }
}

package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.ConnectedAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * tbl_connected_account 접근.
 *
 * ⚠ @Mapper 가 없으면 등록되지 않는다 (@MapperScan(annotationClass = Mapper.class)).
 * SQL 은 mapper/account/ConnectedAccountMapper.xml 에 있다.
 */
@Mapper
public interface ConnectedAccountMapper {

    /** 연결 계좌 목록 (해제되지 않은 것만) */
    List<ConnectedAccount> findActiveByUser(@Param("userId") long userId);

    /** 이미 연결된 계좌의 해시 목록. 계좌 선택 화면의 alreadyLinked 판정에 쓴다 */
    List<String> findActiveHashes(@Param("userId") long userId);

    ConnectedAccount findByIdAndUser(@Param("id") long id, @Param("userId") long userId);

    /**
     * 연결 저장.
     * UNIQUE KEY (user_id, account_no_encrypted) 가 중복을 막는다 —
     * 같은 계좌를 두 번 연결하면 해제 후 재연결로 본다(is_active 를 되살린다).
     */
    int insert(ConnectedAccount account);

    /** 해제했던 계좌를 다시 연결할 때. 새 행을 만들지 않고 되살린다 */
    int reactivate(ConnectedAccount account);

    /** 연결 해제 (soft delete) */
    int deactivate(@Param("id") long id, @Param("userId") long userId);

    /** 사용자의 모든 연결 해제. 금융정보 동의 철회 시 쓴다 (이슈 #13 TODO(#12)) */
    int deactivateAllByUser(@Param("userId") long userId);

    /** 동기화 결과 반영 */
    int updateSync(@Param("id") long id,
                   @Param("userId") long userId,
                   @Param("syncStatus") String syncStatus,
                   @Param("lastSyncAt") LocalDateTime lastSyncAt,
                   @Param("syncFailReason") String syncFailReason);

    /**
     * 조회 성공 반영. **잔액까지 갱신한다.**
     * 예전에는 상태·시각만 바꿔서 "방금 동기화됨" 이라면서 금액은 그대로였다.
     */
    int updateSynced(@Param("id") long id,
                     @Param("userId") long userId,
                     @Param("balance") java.math.BigDecimal balance,
                     @Param("lastSyncAt") LocalDateTime lastSyncAt);
}

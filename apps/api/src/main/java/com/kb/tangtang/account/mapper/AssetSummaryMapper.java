package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetSnapshotRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 자산 현황(GET /api/assets/summary) 집계 전용 조회.
 *
 * ⚠ @Mapper 가 없으면 등록되지 않는다 (@MapperScan(annotationClass = Mapper.class)).
 * SQL 은 mapper/account/AssetSummaryMapper.xml 에 있다.
 */
@Mapper
public interface AssetSummaryMapper {

    /** 연결 해제되지 않은 계좌를 account_type 별로 묶은 개수·금액. 종류가 없으면 행 자체가 없다. */
    List<AssetGroupRow> findAssetGroupsByUser(@Param("userId") long userId);

    /** 지정한 yearMonth 들 중 net_worth 가 채워진 tbl_asset_snapshot 행만 반환한다. */
    List<AssetSnapshotRow> findNetWorthSnapshots(@Param("userId") long userId,
                                                  @Param("yearMonths") List<String> yearMonths);
}

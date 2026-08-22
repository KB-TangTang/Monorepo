package com.kb.tangtang.user.mapper;

import com.kb.tangtang.user.dto.ConsentRecordDto;
import com.kb.tangtang.user.dto.MyConsentRowDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Mapper 는 필수다. RootConfig 가 @MapperScan(annotationClass = Mapper.class) 로
 * 제한하고 있어 붙이지 않으면 빈으로 등록되지 않는다.
 *
 * types 가 빈 리스트면 IN () 이 되어 SQL 문법 오류가 난다.
 * 호출부(ConsentService)에서 빈 리스트를 걸러낸다.
 */
@Mapper
public interface ConsentMapper {

    /** (user_id, consent_type) 유니크 제약 기반 upsert. 재동의·철회 후 재동의가 한 문장으로 처리된다. */
    void upsert(ConsentRecordDto record);

    List<MyConsentRowDto> findByUserId(@Param("userId") Long userId);

    /** 철회. 행을 지우지 않고 UPDATE 한다. 이미 철회된 행은 건드리지 않으므로 0 이 나올 수 있다. */
    int withdraw(@Param("userId") Long userId,
                 @Param("types") List<String> types,
                 @Param("withdrawnAt") LocalDateTime withdrawnAt);

    /** 유효한 동의 건수. status·withdrawn_at·expires_at 을 모두 본다. */
    int countActive(@Param("userId") Long userId,
                    @Param("types") List<String> types,
                    @Param("now") LocalDateTime now);
}

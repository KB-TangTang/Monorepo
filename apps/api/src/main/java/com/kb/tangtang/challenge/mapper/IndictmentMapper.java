package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.Indictment;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code tbl_indictment} 접근.
 *
 * {@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface IndictmentMapper {

    /**
     * 기소 생성. 채번된 id 는 {@code indictment.id} 에 채워진다.
     *
     * <p><b>{@code result_id} 가 이미 기소된 행이면 {@code DuplicateKeyException} 이 난다.</b>
     * {@code uk_ind_result} UNIQUE 때문이고, 배치에서는 <b>정상 경로</b>다 —
     * 5분마다 도는 배치가 같은 위반을 다시 잡거나 두 인스턴스가 동시에 INSERT 하면 발생한다.
     * 호출부는 이 예외를 잡아 알림 발행까지 통째로 건너뛴다. 예외를 잡지 않으면
     * 그룹 하나의 실패가 트랜잭션 전체를 되돌린다.
     * ({@code db/migration/20260815_add_indictment_result_unique.sql})
     *
     * <p>{@code status} 는 항상 {@code DEFENSE_WAIT} 이고
     * {@code result} · {@code verdict_method} · {@code ai_verdict_reason} 은 NULL 이다.
     * DB CHECK(ck_ind_result)가 이 조합만 허용한다.
     */
    int insertIndictment(Indictment indictment);
}

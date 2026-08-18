package com.kb.tangtang.challenge.mapper;

import com.kb.tangtang.challenge.domain.Defense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code tbl_defense} · {@code tbl_defense_images} 접근 (이슈 #170).
 *
 * <p>수정·삭제가 없다. 변론은 한 번 제출하면 끝이고({@code uk_def_indictment}) 기소 상태가
 * {@code VOTING} 으로 넘어가 다시 제출할 수도 없다. 그래서 INSERT 와 조회만 있다.
 *
 * <p>{@code @Mapper} 가 없으면 등록되지 않는다 — RootConfig 가
 * {@code @MapperScan(annotationClass = Mapper.class)} 로 제한돼 있다.
 */
@Mapper
public interface DefenseMapper {

    /**
     * 변론 저장. 채번된 id 는 {@code defense.id} 에 채워진다(이미지 INSERT 에 필요하다).
     *
     * <p>{@code deduction_amount} 는 <b>서버가 계산한 값</b>이다 —
     * {@code 결산 구간 소비액 - actualBurdenAmount}. 클라이언트가 보낸 값을 그대로 넣지 않는다.
     *
     * <p>같은 기소에 두 번 넣으면 {@code uk_def_indictment} 로 {@code DuplicateKeyException} 이
     * 난다. 호출부가 미리 {@link #existsByIndictmentId} 로 걸러 사용자에게 오류 코드를 주지만,
     * 동시 요청이 들어오면 이 UNIQUE 가 마지막 방어선이다.
     */
    int insertDefense(Defense defense);

    /**
     * 증빙 이미지 저장. <b>URL 이 아니라 저장 키</b>를 넣는다
     * ({@code defense/{indictmentId}/{uuid}.jpg}). URL 로 저장하면 로컬 → S3 로 옮길 때
     * 기존 행을 전부 변환해야 한다({@code db/migration/20260812_add_user_profile_image.sql} 근거).
     *
     * @param imageKeys 비어 있으면 호출하지 않는다. {@code foreach} 가 빈 목록으로
     *                  {@code VALUES} 를 만들면 SQL 문법 오류가 난다
     */
    int insertDefenseImages(@Param("defenseId") Long defenseId,
                            @Param("imageKeys") List<String> imageKeys);

    /** 이미 변론했는지. 중복 제출을 사용자에게 오류 코드로 알리는 데 쓴다. */
    boolean existsByIndictmentId(@Param("indictmentId") Long indictmentId);

    /** 없으면 NULL. 재판 상세가 「변론 대기」와 「변론 제출됨」을 이 NULL 로 가른다. */
    Defense findByIndictmentId(@Param("indictmentId") Long indictmentId);

    /**
     * 증빙 이미지의 <b>저장 키</b> 목록. 없으면 빈 목록이다.
     * URL 변환은 서비스가 {@code ImageStorage#urlOf} 로 한다.
     */
    List<String> findImageKeysByDefenseId(@Param("defenseId") Long defenseId);
}

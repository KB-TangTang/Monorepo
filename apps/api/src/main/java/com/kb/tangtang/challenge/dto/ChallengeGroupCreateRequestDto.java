package com.kb.tangtang.challenge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 그룹 챌린지 생성 요청 (GC_01_02 ~ GC_01_04).
 *
 * 정원({@code max_members})은 받지 않는다. 생성 화면에 입력 UI 가 없어 6 으로 고정한다.
 * 프론트의 자유 규칙 입력값(`rules`)은 ERD 컬럼명에 맞춰 {@code memo} 로 보낸다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChallengeGroupCreateRequestDto {

    private String groupName;

    /** NULL = 총소비 챌린지. */
    private Long categoryId;

    /** 0 이상. 0 은 무지출 챌린지로 유효한 값이다. */
    private Integer limitAmount;

    /** DAILY / PERIOD. */
    private String evalType;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 리워드·벌칙 자유 메모. 표시 전용이며 시스템이 집행하지 않는다. */
    private String memo;
}

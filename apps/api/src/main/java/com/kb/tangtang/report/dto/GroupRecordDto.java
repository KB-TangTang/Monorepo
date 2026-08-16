package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

/**
 * 선택 월에 종료되고 최종 결과가 확정된 그룹 챌린지 전적 요약.
 *
 * <p>개인 미션 성과와 별도 집계다. 참여한 확정 그룹이 없으면 상세 리포트의
 * {@code groupRecord} 자체를 {@code null}로 반환한다.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRecordDto {

    private long participatingGroups;
    private long survivedCount;
    private long eliminatedCount;
    private long indictedCount;
    private long acquittedCount;
    private long convictedCount;
}

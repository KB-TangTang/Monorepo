package com.kb.tangtang.mission.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.mission.dto.MissionMonthlyRankingDto;
import com.kb.tangtang.mission.dto.MissionCertificateDto;
import com.kb.tangtang.mission.dto.MissionCertificateTitlesDto;
import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.dto.MissionRankingMonthsDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/** {@code MissionScoreController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.MISSION)
public interface MissionScoreControllerDocs {

    @ApiOperation(value = "이번 달 개인 미션 누적 점수 조회",
            notes = "서울 시간 기준 현재 월의 확정 성공 미션 점수와 연속 성공 보너스를 조회한다.")
    ApiResponse<MissionMonthlyScoreDto> getCurrentMonthlyScore(@ApiIgnore Long userId);

    @ApiOperation(value = "개인 미션 월간 랭킹 조회",
            notes = "yearMonth의 상위 10명과 로그인 사용자의 순위를 조회합니다. "
                    + "yearMonth를 생략하면 서울 시간 기준 현재 월을 사용합니다.")
    ApiResponse<MissionMonthlyRankingDto> getMonthlyRanking(
            @ApiIgnore Long userId,
            String yearMonth);

    @ApiOperation(value = "개인 미션 명예 인증서 조회",
            notes = "랭킹이 확정된 전월 이전의 인증서 데이터를 조회합니다. 당월과 미래 월은 CERTIFICATE_NOT_FINALIZED 오류를 반환합니다.")
    ApiResponse<MissionCertificateDto> getCertificate(@ApiIgnore Long userId, String yearMonth);

    @ApiOperation(value = "개인 미션 인증서 AI 명예 타이틀 조회",
            notes = "매월 1일 생성해 저장한 전월 이전의 AI 명예 타이틀 3개를 조회합니다. "
                    + "아직 생성되지 않았거나 생성에 실패한 경우 FALLBACK 타이틀을 반환합니다.")
    ApiResponse<MissionCertificateTitlesDto> getCertificateTitles(@ApiIgnore Long userId, String yearMonth);

    @ApiOperation(value = "랭킹 보유 월 목록 조회",
            notes = "전체 월간 랭킹 데이터가 존재하는 YYYY-MM 목록을 조회합니다.")
    ApiResponse<MissionRankingMonthsDto> getRankingMonths(@ApiIgnore Long userId);
}

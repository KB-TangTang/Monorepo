package com.kb.tangtang.account.docs;

import com.kb.tangtang.account.dto.AssetAccountDetailDto;
import com.kb.tangtang.account.dto.AssetInvestmentDetailDto;
import com.kb.tangtang.account.dto.AssetLoanDetailDto;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.dto.AssetTrendDto;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code AssetController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.ACCOUNT)
public interface AssetControllerDocs {

    @ApiOperation(value = "자산 현황 요약",
            notes = "순자산·전월 대비 증감·최근 6개월 추이·구성(도넛차트)·종류별 목록을 한 번에 내려준다. "
                    + "모든 값은 응답의 asOf 시각 기준 라이브 잔액으로 계산해 화면 카드 간 금액이 어긋나지 않는다. "
                    + "baseDate 생략 시 오늘 날짜를 기준으로 한다. 형식이 잘못되면 INVALID_REQUEST(400).")
    ApiResponse<AssetSummaryDto> getSummary(@ApiIgnore Long userId,
                                            @ApiParam(value = "기준일 (YYYY-MM-DD). 생략 시 오늘")
                                            String baseDate);

    @ApiOperation(value = "순자산 추이",
            notes = "최근 6개월(baseDate 가 속한 달 포함) 순자산·부채 추이만 내려준다. "
                    + "GET /api/assets/summary 와 같은 계산 로직을 공유하므로 두 응답의 trend 값은 항상 같다. "
                    + "baseDate 생략 시 오늘 날짜를 기준으로 한다. 형식이 잘못되면 INVALID_REQUEST(400).")
    ApiResponse<AssetTrendDto> getTrend(@ApiIgnore Long userId,
                                        @ApiParam(value = "기준일 (YYYY-MM-DD). 생략 시 오늘")
                                        String baseDate);

    @ApiOperation(value = "자산 상세 목록 — 입출금·예적금·페이머니",
            notes = "지정한 종류의 연결 계좌 목록과 잔액 합계를 내려준다. 연결 해제(is_active=0)된 계좌는 제외한다. "
                    + "type 이 DEMAND_DEPOSIT·SAVINGS·PAY_MONEY 중 하나가 아니면 INVALID_REQUEST(400) — "
                    + "투자(SECURITIES)는 GET /api/assets/investments, 대출(LOAN)은 GET /api/assets/loans 를 쓴다.")
    ApiResponse<AssetAccountDetailDto> getAccountsByType(@ApiIgnore Long userId,
                                                          @ApiParam(value = "DEMAND_DEPOSIT | SAVINGS | PAY_MONEY",
                                                                  required = true)
                                                          String type);

    @ApiOperation(value = "자산 상세 목록 — 투자",
            notes = "사용자의 보유 종목과 평가금액·원금 합계를 내려준다. 연결 해제(is_active=0)된 계좌의 "
                    + "종목은 제외한다. 종목별 손익금액·손익률은 tbl_investment_holding 에 동기화된 값을 "
                    + "그대로 반환한다(화면에서 다시 계산하지 않는다).")
    ApiResponse<AssetInvestmentDetailDto> getInvestments(@ApiIgnore Long userId);

    @ApiOperation(value = "자산 상세 목록 — 대출",
            notes = "사용자의 전체 대출 목록과 잔액 합계를 내려준다.")
    ApiResponse<AssetLoanDetailDto> getLoans(@ApiIgnore Long userId);
}

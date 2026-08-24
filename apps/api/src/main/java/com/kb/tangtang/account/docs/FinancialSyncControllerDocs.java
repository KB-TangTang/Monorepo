package com.kb.tangtang.account.docs;

import com.kb.tangtang.account.dto.FinancialSyncRequestDto;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/**
 * {@code FinancialSyncController} 의 Swagger 문서 (이슈 #147).
 *
 * <p>태그를 {@link SwaggerTags#ACCOUNT} 로 둔 이유 — 이 API 는 계좌 연동 흐름의 <b>마지막 단계</b>다.
 * 연동을 마친 {@code LinkDoneView} 가 바로 이것을 호출해 첫 거래내역을 끌어온다. 새 태그를 만들면
 * 같은 흐름이 문서에서 두 섹션으로 쪼개진다({@code SwaggerTags} 의 「태그는 모듈 하나에 하나」 규칙).
 */
@Api(tags = SwaggerTags.ACCOUNT)
public interface FinancialSyncControllerDocs {

    @ApiOperation(value = "금융 자산·거래내역 동기화",
            notes = "연결된 기관에서 잔액과 거래내역을 끌어와 저장하고, 새로 들어온 거래를 규칙으로 분류한다. "
                    + "수집 순서는 BANK · DEPOSIT · SECURITIES · LOAN · PAY_MONEY · CARD 다.\n\n"
                    + "**언제 호출하나** — ① 계좌 연동 완료 직후 최초 동기화(LinkDoneView) "
                    + "② 사용자가 「지금 동기화」를 누를 때. 이 두 경우 말고는 20분 자동 배치가 대신 돈다.\n\n"
                    + "**동기화 범위** — 평소에는 `tbl_connected_account` 에 이미 있는 기관코드로만 정해진다. "
                    + "대출·페이머니는 계좌 연동이 조회하지 못해 그 행이 생기지 않으므로, "
                    + "방금 선택한 기관코드를 `institutionCodes` 로 직접 넘겨 범위를 넓힌다(#334). "
                    + "그 기관의 첫 동기화가 행을 만들고 나면 다음부터는 생략해도 된다.\n\n"
                    + "**사용자가 연결 해제한 계좌(is_active=0)는 되살리지 않는다**(#199).\n\n"
                    + "응답의 `status` 는 성공 시 항상 `COMPLETED` 다. "
                    + "`llmCategorizationStatus` 가 `PENDING` 이면 규칙으로 분류하지 못한 거래가 있어 "
                    + "LLM 분류를 비동기로 등록했다는 뜻이고, `NOT_REQUIRED` 면 대상이 없다는 뜻이다 — "
                    + "어느 쪽이든 이 응답은 기다리지 않는다.\n\n"
                    + "실패하면 수집·저장·분류 어느 단계에서 멎었는지가 `tbl_financial_sync_history` 에 "
                    + "`status=FAILED` 로 남는다(`failed_source` = 소스명 또는 SAVE · CATEGORIZATION).")
    ApiResponse<FinancialSyncResultDto> sync(
            @ApiIgnore Long userId,
            @ApiParam(value = "생략 가능. 대출·페이머니처럼 아직 연결 계좌 행이 없는 기관을 "
                    + "이번 범위에 포함시킬 때만 채운다. 최초 동기화가 아니면 본문 없이 호출한다.")
            FinancialSyncRequestDto request);
}

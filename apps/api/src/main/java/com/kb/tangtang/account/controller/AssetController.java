package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.docs.AssetControllerDocs;
import com.kb.tangtang.account.dto.AssetAccountDetailDto;
import com.kb.tangtang.account.dto.AssetInvestmentDetailDto;
import com.kb.tangtang.account.dto.AssetLoanDetailDto;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.dto.AssetTrendDto;
import com.kb.tangtang.account.service.AssetAccountDetailService;
import com.kb.tangtang.account.service.AssetInvestmentDetailService;
import com.kb.tangtang.account.service.AssetLoanDetailService;
import com.kb.tangtang.account.service.AssetSummaryService;
import com.kb.tangtang.account.service.AssetTrendService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자산 현황 화면(자산 탭 홈 · 종류별 상세)용 API.
 * 사용자 식별은 @LoginUser 로만 한다. 요청으로 userId 를 받지 않는다.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController implements AssetControllerDocs {

    private final AssetSummaryService assetSummaryService;
    private final AssetTrendService assetTrendService;
    private final AssetAccountDetailService assetAccountDetailService;
    private final AssetInvestmentDetailService assetInvestmentDetailService;
    private final AssetLoanDetailService assetLoanDetailService;

    public AssetController(AssetSummaryService assetSummaryService,
                            AssetTrendService assetTrendService,
                            AssetAccountDetailService assetAccountDetailService,
                            AssetInvestmentDetailService assetInvestmentDetailService,
                            AssetLoanDetailService assetLoanDetailService) {
        this.assetSummaryService = assetSummaryService;
        this.assetTrendService = assetTrendService;
        this.assetAccountDetailService = assetAccountDetailService;
        this.assetInvestmentDetailService = assetInvestmentDetailService;
        this.assetLoanDetailService = assetLoanDetailService;
    }

    @GetMapping("/summary")
    public ApiResponse<AssetSummaryDto> getSummary(@LoginUser Long userId,
                                                    @RequestParam(required = false) String baseDate) {
        return ApiResponse.ok(assetSummaryService.getSummary(userId, baseDate));
    }

    @GetMapping("/trend")
    public ApiResponse<AssetTrendDto> getTrend(@LoginUser Long userId,
                                                @RequestParam(required = false) String baseDate) {
        return ApiResponse.ok(assetTrendService.getTrend(userId, baseDate));
    }

    @GetMapping("/accounts")
    public ApiResponse<AssetAccountDetailDto> getAccountsByType(@LoginUser Long userId,
                                                                  @RequestParam String type) {
        return ApiResponse.ok(assetAccountDetailService.getByType(userId, type));
    }

    @GetMapping("/investments")
    public ApiResponse<AssetInvestmentDetailDto> getInvestments(@LoginUser Long userId) {
        return ApiResponse.ok(assetInvestmentDetailService.getInvestments(userId));
    }

    @GetMapping("/loans")
    public ApiResponse<AssetLoanDetailDto> getLoans(@LoginUser Long userId) {
        return ApiResponse.ok(assetLoanDetailService.getLoans(userId));
    }
}

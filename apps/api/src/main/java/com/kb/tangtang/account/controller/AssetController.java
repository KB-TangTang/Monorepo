package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.docs.AssetControllerDocs;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.service.AssetSummaryService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자산 현황 화면(자산 탭 홈)용 API.
 * 사용자 식별은 @LoginUser 로만 한다. 요청으로 userId 를 받지 않는다.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController implements AssetControllerDocs {

    private final AssetSummaryService assetSummaryService;

    public AssetController(AssetSummaryService assetSummaryService) {
        this.assetSummaryService = assetSummaryService;
    }

    @GetMapping("/summary")
    public ApiResponse<AssetSummaryDto> getSummary(@LoginUser Long userId,
                                                    @RequestParam(required = false) String baseDate) {
        return ApiResponse.ok(assetSummaryService.getSummary(userId, baseDate));
    }
}

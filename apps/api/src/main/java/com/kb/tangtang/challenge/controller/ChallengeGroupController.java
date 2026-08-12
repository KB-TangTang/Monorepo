package com.kb.tangtang.challenge.controller;

import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupSummaryDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.service.ChallengeGroupService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 그룹 챌린지(지방법원) — 생성 · 초대 · 참여 · 조회.
 */
@RestController
@RequestMapping("/api/group-challenges")
public class ChallengeGroupController {

    private final ChallengeGroupService challengeGroupService;

    public ChallengeGroupController(ChallengeGroupService challengeGroupService) {
        this.challengeGroupService = challengeGroupService;
    }

    /** 생성 (GC_01_02 ~ GC_01_04). 방장은 자동으로 참여자가 된다. */
    @PostMapping
    public ApiResponse<ChallengeGroupCreatedDto> create(@LoginUser Long userId,
                                                        @RequestBody ChallengeGroupCreateRequestDto request) {
        return ApiResponse.ok(challengeGroupService.create(userId, request));
    }

    /**
     * 내가 참여 중인 목록 (GC_01_01).
     *
     * @param status 반복 또는 콤마 구분. 「종료됨」 탭은 {@code JUDGING,CLOSED} 를 함께 보낸다.
     */
    @GetMapping
    public ApiResponse<List<ChallengeGroupSummaryDto>> findMyGroups(
            @LoginUser Long userId,
            @RequestParam(name = "status", required = false) List<String> status) {
        return ApiResponse.ok(challengeGroupService.findMyGroups(userId, status));
    }

    /** 상세 (GC_01_09). 참여자만 볼 수 있다. */
    @GetMapping("/{groupId}")
    public ApiResponse<ChallengeGroupDetailDto> findDetail(@LoginUser Long userId,
                                                           @PathVariable Long groupId) {
        return ApiResponse.ok(challengeGroupService.findDetail(userId, groupId));
    }

    /**
     * 초대 코드 미리보기 (GC_01_05).
     *
     * 참여할 수 없는 상태도 200 이다. 사유는 {@code reason} 으로 내려간다 —
     * 참여 확인 화면이 그룹 정보를 보여준 뒤 사유를 안내해야 하기 때문이다.
     */
    @GetMapping("/invite-codes/{inviteCode}")
    public ApiResponse<InviteCodePreviewDto> previewInviteCode(@LoginUser Long userId,
                                                               @PathVariable String inviteCode) {
        return ApiResponse.ok(challengeGroupService.previewInviteCode(userId, inviteCode));
    }

    /** 참여 (GC_01_06). 참여 직후 화면이 상세로 넘어가므로 상세를 그대로 돌려준다. */
    @PostMapping("/{groupId}/members")
    public ApiResponse<ChallengeGroupDetailDto> join(@LoginUser Long userId,
                                                     @PathVariable Long groupId) {
        return ApiResponse.ok(challengeGroupService.join(userId, groupId));
    }
}

package com.kb.tangtang.challenge.controller;

import com.kb.tangtang.challenge.docs.ChallengeGroupControllerDocs;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.dto.GroupRankingDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
import com.kb.tangtang.challenge.service.ChallengeGroupDetailService;
import com.kb.tangtang.challenge.service.ChallengeGroupService;
import com.kb.tangtang.challenge.service.GroupRankingService;
import com.kb.tangtang.challenge.service.GroupTrialService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class ChallengeGroupController implements ChallengeGroupControllerDocs {

    private final ChallengeGroupService challengeGroupService;
    private final GroupTrialService groupTrialService;
    private final ChallengeGroupDetailService challengeGroupDetailService;
    private final GroupRankingService groupRankingService;

    public ChallengeGroupController(ChallengeGroupService challengeGroupService,
                                    GroupTrialService groupTrialService,
                                    ChallengeGroupDetailService challengeGroupDetailService,
                                    GroupRankingService groupRankingService) {
        this.challengeGroupService = challengeGroupService;
        this.groupTrialService = groupTrialService;
        this.challengeGroupDetailService = challengeGroupDetailService;
        this.groupRankingService = groupRankingService;
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
    public ApiResponse<List<ChallengeGroupDto>> findMyGroups(
            @LoginUser Long userId,
            @RequestParam(name = "status", required = false) List<String> status) {
        return ApiResponse.ok(challengeGroupService.findMyGroups(userId, status));
    }

    /**
     * 내가 처리해야 하는 재판 (GC_06_01). 홈 「오늘의 할 일」.
     *
     * <p>경로가 {@code /{groupId}} 와 겹쳐 보이지만 Spring 은 리터럴 경로를 변수 경로보다 먼저
     * 매칭한다. 순서를 바꿔도 결과는 같다 — 다만 <b>{@code my-trials} 를 그룹 ID 로 파싱하려다
     * 400 이 나는 것</b>처럼 보이는 사고가 흔해서 여기 적어 둔다.
     */
    @GetMapping("/my-trials")
    public ApiResponse<List<MyTrialDto>> findMyTrials(@LoginUser Long userId) {
        return ApiResponse.ok(groupTrialService.findMyTrials(userId));
    }

    /**
     * 내가 속한 그룹의 진행 중인 재판 전부 (이슈 #432). 지방법원 홈 「재판 현황」.
     *
     * <p>위 {@code /my-trials} 를 넓힌 것이다. 그쪽은 「내가 지금 행동할 수 있는 것」 2가지만
     * 주는데 여기는 6가지를 전부 준다 — 내가 심판받는 중인 재판이 화면에서 사라지지 않도록.
     *
     * <p>{@code /my-trials} 를 지우지 않는다. 전체 보기 바텀시트가 아직 그 얇은 모양을 쓴다.
     */
    @GetMapping("/trials")
    public ApiResponse<List<GroupIndictmentDto>> findAllMyTrials(@LoginUser Long userId) {
        return ApiResponse.ok(groupTrialService.findAllMyTrials(userId));
    }

    /** 상세 (GC_01_09). 참여자만 볼 수 있다. */
    @GetMapping("/{groupId}")
    public ApiResponse<ChallengeGroupDto> findDetail(@LoginUser Long userId,
                                                           @PathVariable Long groupId) {
        return ApiResponse.ok(challengeGroupService.findDetail(userId, groupId));
    }

    /**
     * 상세 화면 한 벌 (GC_01_09). 위의 {@code /{groupId}} 에 재판 카드와 소비 상태를 얹은 것이다.
     *
     * <p>가벼운 쪽을 지우지 않고 둘을 함께 둔다. 참여 직후 응답({@code join})처럼 그룹 정보만
     * 필요한 자리가 있는데, 거기서 재판·소비 집계까지 읽으면 값을 쓰지도 않고 쿼리만 늘어난다.
     */
    @GetMapping("/{groupId}/detail")
    public ApiResponse<ChallengeGroupDetailDto> findFullDetail(@LoginUser Long userId,
                                                               @PathVariable Long groupId) {
        return ApiResponse.ok(challengeGroupDetailService.findDetail(userId, groupId));
    }

    /**
     * 명예 법정 — 생존자 랭킹 (이슈 #173). 참여자만 볼 수 있다.
     *
     * <p>종료(CLOSED) 그룹은 확정 배치(#172)가 남긴 {@code final_*} 를 읽기만 하고,
     * 그 외에는 지금 시점의 순위를 계산한다.
     */
    @GetMapping("/{groupId}/ranking")
    public ApiResponse<GroupRankingDto> findRanking(@LoginUser Long userId,
                                                    @PathVariable Long groupId) {
        return ApiResponse.ok(groupRankingService.findRanking(userId, groupId));
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

    /**
     * 참여 (GC_01_06). 참여 직후 화면이 상세로 넘어가므로 상세를 그대로 돌려준다.
     *
     * <p><b>groupId 가 아니라 초대 코드를 받는다.</b> 예전에는 {@code POST /{groupId}/members} 였는데,
     * 코드 검증이 미리보기(읽기 전용)에만 있고 정작 참여에는 없어 <b>코드 없이 groupId 만으로 참여할 수
     * 있었다</b>(이슈 #346). id 가 AUTO_INCREMENT 연번이라 순회로 모집 중인 방을 전부 찾을 수 있었다.
     *
     * <p>groupId 를 받으면서 코드를 함께 검증하는 방법도 있지만, 그러면 「검증을 빠뜨리면 다시 뚫리는」
     * 구조가 남는다. <b>파라미터 자체를 없애 우회 경로를 지운다.</b>
     */
    @PostMapping("/invite-codes/{inviteCode}/members")
    public ApiResponse<ChallengeGroupDto> join(@LoginUser Long userId,
                                                     @PathVariable String inviteCode) {
        return ApiResponse.ok(challengeGroupService.join(userId, inviteCode));
    }

    /**
     * 삭제 (이슈 #352). <b>방장만</b> 지울 수 있고 <b>모집 중</b>일 때만 지워진다.
     *
     * <p>돌려줄 게 없어 {@code Void} 다. 삭제 뒤에 그룹 정보를 내려주면 「지웠는데 아직 있다」로
     * 읽히고, 프론트는 어차피 목록으로 넘어간다.
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@LoginUser Long userId,
                                         @PathVariable Long groupId) {
        challengeGroupService.deleteGroup(userId, groupId);
        return ApiResponse.ok();
    }
}

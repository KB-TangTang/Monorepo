package com.kb.tangtang.challenge.controller;

import com.kb.tangtang.challenge.docs.GroupTrialControllerDocs;
import com.kb.tangtang.challenge.dto.GroupTrialDetailDto;
import com.kb.tangtang.challenge.dto.TrialTransactionsDto;
import com.kb.tangtang.challenge.service.DefenseService;
import com.kb.tangtang.challenge.service.GroupTrialService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.storage.ImageProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 소비 재판 — 상세 · 결산 구간 거래 목록 · 변론 · 혐의 인정 (이슈 #170).
 *
 * <p>{@link ChallengeGroupController} 에 넣지 않았다. 그쪽은 그룹 자체(생성·초대·참여)를 다루고
 * 6명이 병렬로 작업하는 중이라 같은 파일에서 충돌한다. 경로 접두({@code /trials})가 달라
 * 매핑도 겹치지 않는다.
 *
 * <p>Swagger 태그는 {@code ChallengeGroupController} 와 같다 — 태그는 모듈 단위이고,
 * 컨트롤러가 늘어도 문서 섹션은 하나여야 한다.
 */
@RestController
@RequestMapping("/api/group-challenges/trials")
public class GroupTrialController implements GroupTrialControllerDocs {

    private final GroupTrialService groupTrialService;
    private final DefenseService defenseService;
    private final ImageProcessor imageProcessor;

    public GroupTrialController(GroupTrialService groupTrialService,
                                DefenseService defenseService,
                                ImageProcessor imageProcessor) {
        this.groupTrialService = groupTrialService;
        this.defenseService = defenseService;
        this.imageProcessor = imageProcessor;
    }

    /**
     * 재판 상세. 기소 안내 · 변론 작성 · 투표 · 판결 상세가 공유한다.
     *
     * <p>그룹 참여자면 누구나 볼 수 있다 — 배심원도 봐야 하는 화면이다.
     */
    @GetMapping("/{indictmentId}")
    public ApiResponse<GroupTrialDetailDto> findTrialDetail(@LoginUser Long userId,
                                                            @PathVariable Long indictmentId) {
        return ApiResponse.ok(groupTrialService.findTrialDetail(userId, indictmentId));
    }

    /**
     * 결산 구간의 거래 목록. <b>피고 본인만</b> 볼 수 있다.
     *
     * <p>상세와 합치지 않은 이유는 권한이 다르기 때문이다. 한 응답에 섞으면 배심원에게
     * 남의 거래내역이 흘러간다.
     */
    @GetMapping("/{indictmentId}/transactions")
    public ApiResponse<TrialTransactionsDto> findTrialTransactions(@LoginUser Long userId,
                                                                   @PathVariable Long indictmentId) {
        return ApiResponse.ok(groupTrialService.findTrialTransactions(userId, indictmentId));
    }

    /**
     * 변론 등록 (multipart). 피고 본인만.
     *
     * <p>⚠ <b>{@code getBytes()} 를 부르기 전에 크기부터 본다.</b> 5MB 검사는 바이트 배열을
     * 이미 메모리에 올린 뒤에는 할당 자체를 막지 못한다 — {@code getSize()} 는 본문을 읽지 않고도
     * 알 수 있다. ({@code UserController#uploadProfileImage} 와 같은 이유)
     *
     * <p>그래서 <b>이 검사만 업무 검증보다 먼저 실행된다.</b> 없는 기소에 5MB 초과 사진을 올리면
     * {@code TRIAL_NOT_FOUND} 가 아니라 {@code IMAGE_TOO_LARGE} 가 나간다. 순서를 맞추려면
     * 본문을 먼저 힙에 올려야 해서 감수한 예외다.
     *
     * <p>서비스에는 {@code MultipartFile} 을 넘기지 않는다. 서블릿 타입이 서비스 계층에 들어가면
     * 테스트가 웹 컨테이너 목을 만들어야 한다.
     */
    @PostMapping("/{indictmentId}/defense")
    public ApiResponse<Void> registerDefense(@LoginUser Long userId,
                                             @PathVariable Long indictmentId,
                                             @RequestParam("content") String content,
                                             @RequestParam("actualBurdenAmount") BigDecimal actualBurdenAmount,
                                             @RequestParam(value = "images", required = false)
                                                     List<MultipartFile> images) throws IOException {
        List<byte[]> contents = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    continue;
                }
                imageProcessor.requireWithinLimit(image.getSize());
                contents.add(image.getBytes());
            }
        }

        defenseService.registerDefense(userId, indictmentId, content, actualBurdenAmount, contents);
        return ApiResponse.ok();
    }

    /**
     * 혐의 인정. 피고 본인만, 되돌릴 수 없다.
     *
     * <p>요청 본문이 없다 — 인정한다는 사실 외에 받을 값이 없다. 목숨 차감은 여기서 하지 않는다.
     */
    @PostMapping("/{indictmentId}/confession")
    public ApiResponse<Void> confess(@LoginUser Long userId,
                                     @PathVariable Long indictmentId) {
        defenseService.confess(userId, indictmentId);
        return ApiResponse.ok();
    }
}

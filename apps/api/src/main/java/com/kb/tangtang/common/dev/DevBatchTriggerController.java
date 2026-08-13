package com.kb.tangtang.common.dev;

import com.kb.tangtang.challenge.service.ChallengeGroupStatusBatchService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 배치 수동 트리거 (DEV 전용, 이슈 #152).
 *
 * <p>시연과 검증 때문에 있다. 그룹 챌린지 흐름은 자정 배치 → 평가·기소 → 변론 → 투표 → 판결로
 * 이어지는데, 각 단계가 자정이나 수 시간 뒤에 도는 배치에 걸려 있어 <b>기다려서는 검증할 수
 * 없다.</b> 이 엔드포인트로 기준일을 넣어 원하는 단계를 즉시 돌린다.
 *
 * <p><b>로컬에서만 동작한다</b> — {@link DevEnvironmentGuard} 가 {@code app.env} 로 막는다.
 * 인증도 필요하다. 인터셉터가 {@code /api/**} 에 걸려 있다.
 *
 * <p>배치가 늘어나면({@code #168} 평가·기소, {@code #170} 변론 마감, {@code #172} 개표)
 * {@code switch} 에 이름을 추가한다. 등록 인터페이스를 두지 않은 이유는 배치가 서너 개뿐이라
 * 이름 목록이 한눈에 보이는 편이 낫기 때문이다.
 */
@RestController
@RequestMapping("/api/dev/batches")
@Log4j2
public class DevBatchTriggerController {

    private final DevEnvironmentGuard guard;
    private final ChallengeGroupStatusBatchService challengeGroupStatusBatchService;

    public DevBatchTriggerController(DevEnvironmentGuard guard,
                                     ChallengeGroupStatusBatchService challengeGroupStatusBatchService) {
        this.guard = guard;
        this.challengeGroupStatusBatchService = challengeGroupStatusBatchService;
    }

    /**
     * 배치를 즉시 실행한다.
     *
     * @param name 배치 이름. {@code group-challenge-status}
     * @param date 기준일. 없으면 오늘. 미래 날짜를 넣으면 그날 시작하는 챌린지까지 당겨 처리한다
     * @return 배치가 처리한 건수
     */
    @PostMapping("/{name}")
    public ApiResponse<Map<String, Object>> run(
            @LoginUser Long userId,
            @PathVariable String name,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        guard.ensureLocal();

        LocalDate baseDate = date == null ? LocalDate.now() : date;
        log.warn("DEV 배치 수동 실행 name={} baseDate={} userId={}", name, baseDate, userId);

        int affected = switch (name) {
            case "group-challenge-status" -> challengeGroupStatusBatchService.startDueGroups(baseDate);
            default -> throw new BusinessException("DEV_BATCH_NOT_FOUND",
                    "알 수 없는 배치 이름이에요: " + name);
        };

        return ApiResponse.ok(Map.of("batch", name, "baseDate", baseDate.toString(), "affected", affected));
    }
}

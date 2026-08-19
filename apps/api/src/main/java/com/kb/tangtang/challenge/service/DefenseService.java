package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.Defense;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.common.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 변론 등록 · 혐의 인정 (이슈 #170). 재판 상태를 바꾸는 쪽이다.
 *
 * <p>{@link GroupTrialService} 와 나눈 이유는 그쪽이 {@code @Transactional(readOnly = true)}
 * 전용이라서다. 읽기와 쓰기를 한 클래스에 두면 메서드마다 readOnly 여부를 확인해야 한다.
 *
 * <p><b>기소 조회는 {@link IndictmentMapper#findTrialDetail} 하나로 끝낸다.</b> 검증에 필요한
 * 상태·피고·생성시각과 부담금 상한({@code currentAmount})이 모두 그 한 문장에 있다. 검증용
 * 쿼리를 따로 만들면 소비액 공식이 다섯 벌이 된다(#170 이 그 복사본을 정리하는 이슈다).
 *
 * <p><b>무죄 감액 기록은 여기서 하지 않는다.</b> 판결 확정 후처리는 {@link GroupVerdictTransitionService}
 * 한 곳에 있다. 혐의 인정의 목숨 차감도 그쪽 메서드를 불러서 한다 — 차감 코드를 여기 복사하면
 * 개표 경로와 두 벌이 되어 한쪽만 고쳐진다(이슈 #305).
 */
@Service
public class DefenseService {

    /** 화면(`DefenseWriteView.vue` 의 MAX_TEXT)과 같은 값. 클라이언트만 믿지 않는다. */
    private static final int CONTENT_MAX_LENGTH = 150;

    /** 화면(`DefenseWriteView.vue` 의 MAX_IMAGES)과 같은 값. */
    private static final int MAX_IMAGES = 3;

    private final IndictmentMapper indictmentMapper;
    private final DefenseMapper defenseMapper;
    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;
    private final ApplicationEventPublisher events;
    private final GroupVerdictTransitionService verdictTransition;
    private final Clock clock;

    /** 기소 후 변론을 낼 수 있는 시간. {@link GroupTrialService} 와 같은 프로퍼티를 읽는다. */
    private final int defenseHours;

    @Autowired
    public DefenseService(IndictmentMapper indictmentMapper,
                          DefenseMapper defenseMapper,
                          ImageStorage imageStorage,
                          ImageProcessor imageProcessor,
                          ApplicationEventPublisher events,
                          GroupVerdictTransitionService verdictTransition,
                          @Value("${challenge.trial.defense-hours}") int defenseHours) {
        this(indictmentMapper, defenseMapper, imageStorage, imageProcessor, events,
                verdictTransition, defenseHours, Clock.systemDefaultZone());
    }

    DefenseService(IndictmentMapper indictmentMapper,
                   DefenseMapper defenseMapper,
                   ImageStorage imageStorage,
                   ImageProcessor imageProcessor,
                   ApplicationEventPublisher events,
                   GroupVerdictTransitionService verdictTransition,
                   int defenseHours,
                   Clock clock) {
        this.indictmentMapper = indictmentMapper;
        this.defenseMapper = defenseMapper;
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
        this.events = events;
        this.verdictTransition = verdictTransition;
        this.defenseHours = defenseHours;
        this.clock = clock;
    }

    /**
     * 변론 등록. 성공하면 기소가 {@code DEFENSE_WAIT} → {@code VOTING} 으로 넘어간다.
     *
     * <p><b>{@code deductionAmount} 는 클라이언트를 믿지 않고 서버가 계산한다</b> —
     * {@code 결산 구간 소비액 - actualBurdenAmount}. 화면의 건별 입력은 순수 UI 이고
     * 합계만 올라온다(거래-변론 연결 테이블을 만들지 않은 이유는 이슈 #170 본문에 있다).
     *
     * <p>이미지는 <b>키</b>로 저장한다({@code defense/{indictmentId}/{uuid}.jpg}). URL 로 저장하면
     * 로컬 → S3 로 옮길 때 기존 행을 전부 변환해야 한다
     * ({@code db/migration/20260812_add_user_profile_image.sql} 의 결정).
     * {@code defenseId} 대신 {@code indictmentId} 를 쓰는 이유는 INSERT 전에 키를 만들 수 있어서고,
     * {@code uk_def_indictment} 로 기소당 변론이 1건이라 충돌하지 않는다.
     *
     * @param images NULL 이거나 빈 목록이면 증빙 없이 등록한다
     */
    @Transactional
    public void registerDefense(long userId, long indictmentId, String content,
                                BigDecimal actualBurdenAmount, List<byte[]> images) {
        GroupTrialDetailRow row = requireDefendableIndictment(userId, indictmentId);

        if (defenseMapper.existsByIndictmentId(indictmentId)) {
            throw new BusinessException("DEFENSE_ALREADY_EXISTS", "이미 변론을 제출했습니다.");
        }

        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("DEFENSE_CONTENT_REQUIRED", "변론 내용을 입력해주세요.");
        }
        if (text.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException("DEFENSE_CONTENT_TOO_LONG",
                    CONTENT_MAX_LENGTH + "자 이내로 입력해주세요.");
        }

        BigDecimal burden = actualBurdenAmount == null ? BigDecimal.ZERO : actualBurdenAmount;
        BigDecimal consumption = row.getCurrentAmount() == null
                ? BigDecimal.ZERO : row.getCurrentAmount();
        if (burden.signum() < 0 || burden.compareTo(consumption) > 0) {
            throw new BusinessException("INVALID_BURDEN_AMOUNT",
                    "실제 부담금은 0원 이상, 소비액 이하여야 합니다.");
        }

        List<byte[]> sources = images == null ? List.of() : images;
        if (sources.size() > MAX_IMAGES) {
            throw new BusinessException("TOO_MANY_IMAGES",
                    "증빙 사진은 " + MAX_IMAGES + "장까지 올릴 수 있어요.");
        }

        Defense defense = new Defense();
        defense.setIndictmentId(indictmentId);
        defense.setContent(text);
        defense.setActualBurdenAmount(burden);
        defense.setDeductionAmount(consumption.subtract(burden));
        defenseMapper.insertDefense(defense);

        List<String> imageKeys = storeImages(indictmentId, sources);
        if (!imageKeys.isEmpty()) {
            defenseMapper.insertDefenseImages(defense.getId(), imageKeys);
        }

        /*
         * 여기까지 왔어도 상태가 이미 옮겨졌을 수 있다 — 위 SELECT 와 이 UPDATE 사이에
         * 마감 배치가 끼어들거나 사용자가 제출을 두 번 누르면 그렇다. 0행이면 롤백한다.
         * (변론 INSERT 도 함께 되돌아간다. 트랜잭션이 하나다)
         */
        if (indictmentMapper.moveToVoting(indictmentId) == 0) {
            throw new BusinessException("DEFENSE_NOT_ALLOWED", "지금은 변론할 수 없는 재판입니다.");
        }

        events.publishEvent(new GroupTrialEvents.DefenseRegistered(
                row.getGroupId(), indictmentId, row.getNickname()));
    }

    /**
     * 혐의 인정. {@code DEFENSE_WAIT} → {@code GUILTY}({@code verdict_method = 'CONFESSION'}).
     *
     * <p>변론·투표 행을 만들지 않는다({@code db/schema.sql:505-508} 계약).
     *
     * <p><b>목숨 차감은 개표 경로와 같은 메서드를 부른다</b>
     * ({@link GroupVerdictTransitionService#deductLifeForGuilty}). 이슈 #172 는 후처리를 한 곳에
     * 두려고 이 경로에서 차감을 뺐는데, 그 「한 곳」이 {@code status = 'VOTING'} 인 기소만 훑는
     * 개표 배치라서 혐의 인정 건은 어디서도 깎이지 않았다(이슈 #305). 일일평가 탈락 판정이
     * {@code lives_count = 0} 만 보기 때문에, 혐의를 인정한 참여자는 유죄를 몇 번 받아도
     * 탈락하지 않는 상태였다.
     *
     * <p>UPDATE 가 0행이면 그 자리에서 예외를 던져 차감까지 함께 롤백된다 — 제출을 두 번 눌러도
     * 목숨이 두 번 깎이지 않는다({@code confirmConfession} 의 {@code status = 'DEFENSE_WAIT'} 조건).
     */
    @Transactional
    public void confess(long userId, long indictmentId) {
        GroupTrialDetailRow row = requireDefendableIndictment(userId, indictmentId);

        if (indictmentMapper.confirmConfession(indictmentId) == 0) {
            throw new BusinessException("DEFENSE_NOT_ALLOWED", "지금은 혐의를 인정할 수 없는 재판입니다.");
        }

        int livesLost = verdictTransition.deductLifeForGuilty(row.getGroupId(), row.getUserId());

        events.publishEvent(GroupTrialEvents.VerdictConfirmed.byConfession(
                row.getGroupId(), indictmentId,
                row.getNickname() + "님이 혐의를 인정했어요. 유죄로 확정됩니다.", livesLost));
    }

    /**
     * 변론·혐의 인정에 공통인 검증 1~4. <b>순서를 지킨다</b> —
     * 없는 기소를 「권한 없음」으로 알려 주면 남의 그룹에 그 기소가 있다는 사실이 새어 나간다.
     *
     * <p>마감은 저장값이 아니라 {@code created_at + defenseHours} 계산값이다
     * ({@link GroupTrialService} 와 같은 프로퍼티를 읽는다). 상태 전이 배치가 5분마다 도는
     * 사이에 마감이 지난 건이 남아 있으므로 <b>상태만 보면 부족하다.</b>
     */
    private GroupTrialDetailRow requireDefendableIndictment(long userId, long indictmentId) {
        GroupTrialDetailRow row = indictmentMapper.findTrialDetail(indictmentId, userId);
        if (row == null) {
            throw new BusinessException("TRIAL_NOT_FOUND", "재판을 찾을 수 없습니다.");
        }
        if (row.getUserId() == null || row.getUserId() != userId) {
            throw new BusinessException("NOT_INDICTMENT_OWNER", "본인의 재판만 변론할 수 있습니다.");
        }
        if (!IndictmentStatus.DEFENSE_WAIT.name().equals(row.getStatus())) {
            throw new BusinessException("DEFENSE_NOT_ALLOWED", "변론 기간이 아닙니다.");
        }
        if (row.getCreatedAt().plusHours(defenseHours).isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException("DEFENSE_CLOSED", "변론 마감 시간이 지났습니다.");
        }
        return row;
    }

    /**
     * 증빙 사진을 규격에 맞춰 다시 굽고 저장한다.
     *
     * <p>{@code toSquareJpeg} 가 아니라 {@code toBoundedJpeg} 를 쓴다 — 아바타용 256x256 센터
     * 크롭은 영수증의 위아래를 잘라 증빙 구실을 못 한다.
     *
     * <p>크기 상한({@code IMAGE_TOO_LARGE})은 {@code toBoundedJpeg} 가 안에서 검사한다.
     * 컨트롤러가 {@code MultipartFile#getSize()} 로 먼저 한 번 막지만, 그것은 힙 할당을
     * 피하기 위한 것이고 이 계층으로 직접 들어오는 호출(테스트)도 같이 막혀야 한다.
     *
     * <p>실패하면 트랜잭션이 DB 를 되돌리지만 <b>이미 저장한 파일은 남는다.</b> 참조 없는 고아
     * 파일이 되므로 여기서 직접 지운다 — {@code delete} 는 멱등이라 정리가 실패할 일은 없다.
     */
    private List<String> storeImages(long indictmentId, List<byte[]> sources) {
        List<String> keys = new ArrayList<>();
        try {
            for (byte[] source : sources) {
                String key = "defense/" + indictmentId + "/" + UUID.randomUUID() + ".jpg";
                imageStorage.store(imageProcessor.toBoundedJpeg(source), key);
                keys.add(key);
            }
        } catch (RuntimeException e) {
            for (String key : keys) {
                imageStorage.delete(key);
            }
            throw e;
        }
        return keys;
    }
}

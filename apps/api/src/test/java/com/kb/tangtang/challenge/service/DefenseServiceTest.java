package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.Defense;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.common.storage.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 변론 등록 · 혐의 인정 (이슈 #170).
 *
 * <p>SQL 의 모양은 {@code ChallengeMapperXmlTest} 가 지킨다. 여기서는 <b>SQL 이 만들 수 없는 것</b>
 * — 검증 순서, 마감 시각 계산, 서버가 계산하는 {@code deductionAmount}, 조건부 UPDATE 가 0행을
 * 돌려줬을 때의 롤백 — 만 본다.
 *
 * <p>{@link Clock} 을 주입해 마감을 고정한다. {@code LocalDateTime.now()} 를 쓰면
 * 「마감 6시간」 테스트가 실행 시각에 따라 통과·실패를 오간다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefenseServiceTest {

    private static final int DEFENSE_HOURS = 6;
    private static final long USER_ID = 7L;
    private static final long INDICTMENT_ID = 11L;
    private static final long GROUP_ID = 3L;

    /** 기소 시각 10:00, 「지금」 12:00 → 마감(16:00) 전. */
    private static final LocalDateTime INDICTED_AT = LocalDateTime.of(2026, 8, 18, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 12, 0);

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private DefenseMapper defenseMapper;
    @Mock private ImageStorage imageStorage;
    @Mock private ImageProcessor imageProcessor;
    @Mock private ApplicationEventPublisher events;

    private DefenseService service() {
        return service(NOW);
    }

    private DefenseService service(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        return new DefenseService(indictmentMapper, defenseMapper, imageStorage, imageProcessor,
                events, DEFENSE_HOURS, clock);
    }

    /** 검증을 모두 통과하는 기본 상태 — 각 테스트가 필요한 필드만 어긋나게 바꾼다. */
    private GroupTrialDetailRow defendableRow() {
        GroupTrialDetailRow row = new GroupTrialDetailRow();
        row.setIndictmentId(INDICTMENT_ID);
        row.setGroupId(GROUP_ID);
        row.setUserId(USER_ID);
        row.setNickname("지판");
        row.setStatus("DEFENSE_WAIT");
        row.setCreatedAt(INDICTED_AT);
        row.setLimitAmount(new BigDecimal("50000"));
        row.setCurrentAmount(new BigDecimal("55000"));
        return row;
    }

    private void given(GroupTrialDetailRow row) {
        when(indictmentMapper.findTrialDetail(INDICTMENT_ID, USER_ID)).thenReturn(row);
        when(indictmentMapper.moveToVoting(INDICTMENT_ID)).thenReturn(1);
        when(indictmentMapper.confirmConfession(INDICTMENT_ID)).thenReturn(1);
    }

    private BusinessException callDefense(String content, BigDecimal burden, List<byte[]> images) {
        return assertThrows(BusinessException.class,
                () -> service().registerDefense(USER_ID, INDICTMENT_ID, content, burden, images));
    }

    // ── 검증 순서 ──────────────────────────────────────────────────────────

    /**
     * <b>없는 기소와 남의 기소를 다른 코드로 알려 주면 안 된다.</b> 조회 SQL 이 참여자 조인으로
     * 걸러 NULL 을 주므로, 그룹 밖 사람에게는 존재 자체가 보이지 않는다.
     */
    @Test
    @DisplayName("기소가 없으면 TRIAL_NOT_FOUND")
    void rejectsMissingIndictment() {
        when(indictmentMapper.findTrialDetail(INDICTMENT_ID, USER_ID)).thenReturn(null);

        assertEquals("TRIAL_NOT_FOUND",
                callDefense("억울합니다", BigDecimal.ZERO, List.of()).getCode());
    }

    /** 같은 그룹 사람은 행을 받지만 피고가 아니면 변론할 수 없다. */
    @Test
    @DisplayName("피고 본인이 아니면 NOT_INDICTMENT_OWNER")
    void rejectsNonDefendant() {
        GroupTrialDetailRow row = defendableRow();
        row.setUserId(99L);
        given(row);

        assertEquals("NOT_INDICTMENT_OWNER",
                callDefense("억울합니다", BigDecimal.ZERO, List.of()).getCode());
    }

    @Test
    @DisplayName("변론 기간이 아니면 DEFENSE_NOT_ALLOWED")
    void rejectsWrongStatus() {
        GroupTrialDetailRow row = defendableRow();
        row.setStatus("VOTING");
        given(row);

        assertEquals("DEFENSE_NOT_ALLOWED",
                callDefense("억울합니다", BigDecimal.ZERO, List.of()).getCode());
    }

    /**
     * 마감 배치가 5분마다 도는 사이에 <b>상태는 DEFENSE_WAIT 인데 마감은 지난</b> 구간이 생긴다.
     * 상태만 보면 그 구간에 변론이 통과한다.
     */
    @Test
    @DisplayName("상태가 DEFENSE_WAIT 여도 마감이 지났으면 DEFENSE_CLOSED")
    void rejectsExpiredDeadlineEvenWhenStatusStillWaits() {
        given(defendableRow());

        /* 기소 10:00 + 6시간 = 16:00 → 16:01 은 지났다 */
        BusinessException e = assertThrows(BusinessException.class, () ->
                service(LocalDateTime.of(2026, 8, 18, 16, 1))
                        .registerDefense(USER_ID, INDICTMENT_ID, "억울합니다", BigDecimal.ZERO, List.of()));

        assertEquals("DEFENSE_CLOSED", e.getCode());
    }

    /** 마감 시각 정각은 아직 마감이 아니다 ({@code isBefore} 라 경계가 포함된다). */
    @Test
    @DisplayName("마감 정각은 아직 변론할 수 있다")
    void allowsDefenseAtExactDeadline() {
        given(defendableRow());

        service(LocalDateTime.of(2026, 8, 18, 16, 0))
                .registerDefense(USER_ID, INDICTMENT_ID, "억울합니다", BigDecimal.ZERO, List.of());

        verify(indictmentMapper).moveToVoting(INDICTMENT_ID);
    }

    /** {@code uk_def_indictment} 가 최후 방어지만, 유니크 위반은 500 으로 나가 코드를 줄 수 없다. */
    @Test
    @DisplayName("이미 변론을 냈으면 DEFENSE_ALREADY_EXISTS")
    void rejectsDuplicateDefense() {
        given(defendableRow());
        when(defenseMapper.existsByIndictmentId(INDICTMENT_ID)).thenReturn(true);

        assertEquals("DEFENSE_ALREADY_EXISTS",
                callDefense("억울합니다", BigDecimal.ZERO, List.of()).getCode());
    }

    @Test
    @DisplayName("변론 내용이 공백만이면 DEFENSE_CONTENT_REQUIRED")
    void rejectsBlankContent() {
        given(defendableRow());

        assertEquals("DEFENSE_CONTENT_REQUIRED",
                callDefense("   ", BigDecimal.ZERO, List.of()).getCode());
    }

    /** 화면의 {@code maxlength} 는 우회할 수 있다. 서버 상한이 화면과 같은 150 이어야 한다. */
    @Test
    @DisplayName("변론 내용이 150자를 넘으면 DEFENSE_CONTENT_TOO_LONG")
    void rejectsTooLongContent() {
        given(defendableRow());

        assertEquals("DEFENSE_CONTENT_TOO_LONG",
                callDefense("가".repeat(151), BigDecimal.ZERO, List.of()).getCode());
        /* 150자는 통과한다 — 경계에서 한 글자 어긋나면 화면이 못 내는 값이 된다 */
        service().registerDefense(USER_ID, INDICTMENT_ID, "가".repeat(150),
                BigDecimal.ZERO, List.of());
    }

    /**
     * 부담금이 소비액을 넘으면 {@code deductionAmount} 가 음수가 되어 감액이 <b>소비를 늘린다.</b>
     * DB CHECK({@code ck_def_deduction})도 막지만 그때는 500 이다.
     */
    @Test
    @DisplayName("실제 부담금이 소비액을 넘거나 음수면 INVALID_BURDEN_AMOUNT")
    void rejectsBurdenOutOfRange() {
        given(defendableRow());

        assertEquals("INVALID_BURDEN_AMOUNT",
                callDefense("억울합니다", new BigDecimal("55001"), List.of()).getCode());
        assertEquals("INVALID_BURDEN_AMOUNT",
                callDefense("억울합니다", new BigDecimal("-1"), List.of()).getCode());
    }

    @Test
    @DisplayName("증빙 사진이 3장을 넘으면 TOO_MANY_IMAGES")
    void rejectsTooManyImages() {
        given(defendableRow());

        List<byte[]> four = List.of(new byte[] {1}, new byte[] {2}, new byte[] {3}, new byte[] {4});

        assertEquals("TOO_MANY_IMAGES",
                callDefense("억울합니다", BigDecimal.ZERO, four).getCode());
        /* 한 장도 저장하기 전에 막혀야 한다 — 통과 못 할 요청의 파일이 스토리지에 남으면 고아가 된다 */
        verify(imageStorage, never()).store(any(), anyString());
    }

    // ── 서버 계산 · 저장 ───────────────────────────────────────────────────

    /**
     * {@code deductionAmount} 는 클라이언트가 보내지 않는다. 화면의 건별 입력은 UI 이고
     * 합계만 올라오므로 <b>감액은 서버가 소비액에서 뺀다.</b>
     */
    @Test
    @DisplayName("감액은 소비액에서 실제 부담금을 뺀 값으로 서버가 계산한다")
    void computesDeductionOnServer() {
        given(defendableRow());

        service().registerDefense(USER_ID, INDICTMENT_ID, "친구들 몫을 대납했어요",
                new BigDecimal("15000"), List.of());

        ArgumentCaptor<Defense> captor = ArgumentCaptor.forClass(Defense.class);
        verify(defenseMapper).insertDefense(captor.capture());

        Defense saved = captor.getValue();
        assertEquals(0, new BigDecimal("15000").compareTo(saved.getActualBurdenAmount()));
        /* 55,000 − 15,000 */
        assertEquals(0, new BigDecimal("40000").compareTo(saved.getDeductionAmount()));
        assertEquals("친구들 몫을 대납했어요", saved.getContent());
    }

    /**
     * <b>URL 이 아니라 키를 저장한다.</b> URL 로 저장하면 로컬 → S3 로 옮길 때 기존 행을 전부
     * 변환해야 한다({@code 20260812_add_user_profile_image.sql} 의 결정).
     */
    @Test
    @DisplayName("증빙 사진은 기소 ID 로 만든 키로 저장한다")
    void storesImagesAsKeys() {
        given(defendableRow());
        when(imageProcessor.toBoundedJpeg(any())).thenReturn(new byte[] {9});

        service().registerDefense(USER_ID, INDICTMENT_ID, "영수증 첨부",
                BigDecimal.ZERO, List.of(new byte[] {1}));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).store(any(), key.capture());

        assertTrue(key.getValue().startsWith("defense/" + INDICTMENT_ID + "/"),
                "키에 기소 ID 가 없으면 어느 재판의 증빙인지 스토리지만 보고 알 수 없다");
        assertTrue(key.getValue().endsWith(".jpg"));
        assertTrue(key.getValue().length() <= 100,
                "image_url 이 VARCHAR(100) 이라 넘치면 INSERT 가 잘린다");
    }

    /**
     * 아바타용 {@code toSquareJpeg}(256x256 센터 크롭)를 쓰면 <b>영수증의 위아래가 잘려</b>
     * 증빙 구실을 못 한다.
     */
    @Test
    @DisplayName("증빙 사진은 센터 크롭이 아닌 긴 변 제한 규격으로 다시 굽는다")
    void reencodesEvidenceWithoutCropping() {
        given(defendableRow());
        when(imageProcessor.toBoundedJpeg(any())).thenReturn(new byte[] {9});

        service().registerDefense(USER_ID, INDICTMENT_ID, "영수증 첨부",
                BigDecimal.ZERO, List.of(new byte[] {1}));

        verify(imageProcessor).toBoundedJpeg(any());
        verify(imageProcessor, never()).toSquareJpeg(any());
    }

    /**
     * 트랜잭션은 DB 만 되돌린다. 이미 올라간 파일은 참조 없는 고아로 남으므로 직접 지운다.
     */
    @Test
    @DisplayName("두 번째 사진 저장이 실패하면 먼저 올린 사진을 지운다")
    void deletesOrphanImagesWhenLaterUploadFails() {
        given(defendableRow());
        when(imageProcessor.toBoundedJpeg(any()))
                .thenReturn(new byte[] {9})
                .thenThrow(new BusinessException("INVALID_IMAGE", "이미지 형식이 아닙니다."));

        assertEquals("INVALID_IMAGE",
                callDefense("영수증 첨부", BigDecimal.ZERO,
                        List.of(new byte[] {1}, new byte[] {2})).getCode());

        verify(imageStorage).delete(anyString());
    }

    // ── 조건부 UPDATE ─────────────────────────────────────────────────────

    /**
     * SELECT 와 UPDATE 사이에 마감 배치가 끼어들거나 사용자가 제출을 두 번 누르면 상태가 이미
     * 옮겨져 있다. <b>0행이면 예외로 롤백해</b> 변론 INSERT 까지 되돌린다 — 조건 없이 UPDATE 하면
     * 이미 확정된 판결을 되돌려 쓴다.
     */
    @Test
    @DisplayName("상태 전이가 0행이면 DEFENSE_NOT_ALLOWED 로 롤백한다")
    void rollsBackWhenStatusAlreadyMoved() {
        given(defendableRow());
        when(indictmentMapper.moveToVoting(INDICTMENT_ID)).thenReturn(0);

        assertEquals("DEFENSE_NOT_ALLOWED",
                callDefense("억울합니다", BigDecimal.ZERO, List.of()).getCode());
        /* 롤백될 트랜잭션에서 채팅 메시지가 나가면 없던 변론이 그룹 채팅에 남는다 */
        verify(events, never()).publishEvent(any(GroupTrialEvents.DefenseRegistered.class));
    }

    @Test
    @DisplayName("변론 등록 성공 시 채팅용 이벤트를 발행한다")
    void publishesDefenseRegisteredEvent() {
        given(defendableRow());

        service().registerDefense(USER_ID, INDICTMENT_ID, "억울합니다", BigDecimal.ZERO, List.of());

        ArgumentCaptor<GroupTrialEvents.DefenseRegistered> captor =
                ArgumentCaptor.forClass(GroupTrialEvents.DefenseRegistered.class);
        verify(events).publishEvent(captor.capture());

        assertEquals(GROUP_ID, captor.getValue().getGroupId());
        assertEquals(INDICTMENT_ID, captor.getValue().getIndictmentId());
        assertEquals("지판", captor.getValue().getTargetNickname());
    }

    // ── 혐의 인정 ─────────────────────────────────────────────────────────

    /**
     * 인정은 변론·투표 행을 만들지 않는다({@code db/schema.sql:505-508} 계약).
     * 목숨 차감도 없다 — 판결 확정 후처리는 이슈 #172 가 한 곳에서 맡는다.
     */
    @Test
    @DisplayName("혐의 인정은 변론·이미지 행을 만들지 않는다")
    void confessionWritesNoDefenseRow() {
        given(defendableRow());

        service().confess(USER_ID, INDICTMENT_ID);

        verify(indictmentMapper).confirmConfession(INDICTMENT_ID);
        verify(defenseMapper, never()).insertDefense(any());
        verify(defenseMapper, never()).insertDefenseImages(anyLong(), any());
    }

    @Test
    @DisplayName("혐의 인정도 검증 1~4 를 그대로 거친다")
    void confessionSharesValidations() {
        GroupTrialDetailRow row = defendableRow();
        row.setUserId(99L);
        given(row);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().confess(USER_ID, INDICTMENT_ID));

        assertEquals("NOT_INDICTMENT_OWNER", e.getCode());
    }

    @Test
    @DisplayName("혐의 인정 상태 전이가 0행이면 DEFENSE_NOT_ALLOWED")
    void confessionRollsBackWhenStatusAlreadyMoved() {
        given(defendableRow());
        when(indictmentMapper.confirmConfession(INDICTMENT_ID)).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().confess(USER_ID, INDICTMENT_ID));

        assertEquals("DEFENSE_NOT_ALLOWED", e.getCode());
        verify(events, never()).publishEvent(any(GroupTrialEvents.VerdictConfirmed.class));
    }

    /** {@code ChatSystemMessageListener} 는 {@code summary} 를 그대로 채팅에 올린다. */
    @Test
    @DisplayName("혐의 인정 이벤트는 채팅에 그대로 올라갈 문장을 담는다")
    void confessionEventCarriesReadySentence() {
        given(defendableRow());

        service().confess(USER_ID, INDICTMENT_ID);

        ArgumentCaptor<GroupTrialEvents.VerdictConfirmed> captor =
                ArgumentCaptor.forClass(GroupTrialEvents.VerdictConfirmed.class);
        verify(events).publishEvent(captor.capture());

        assertEquals(GROUP_ID, captor.getValue().getGroupId());
        assertTrue(captor.getValue().getSummary().startsWith("지판"),
                "리스너가 summary 를 그대로 올리므로 닉네임이 들어 있어야 한다");
    }
}

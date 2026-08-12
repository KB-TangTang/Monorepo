package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.user.domain.TutorialType;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 실명 갱신 규칙 검증.
 * 프론트(utils/account.js validateSimpleAuthForm)와 같은 규칙을 서버에서도 본다 —
 * 화면을 거치지 않는 요청이 들어올 수 있기 때문이다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 7L;

    @Mock private UserMapper userMapper;
    @Mock private ProfileImageUrlResolver profileImageUrlResolver;
    @Mock private ImageStorage imageStorage;
    @Mock private ImageProcessor imageProcessor;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userMapper, profileImageUrlResolver, imageStorage, imageProcessor);
    }

    private static UserDto user(String name) {
        return UserDto.builder()
                .id(USER_ID)
                .socialProvider("GOOGLE")
                .providerUserId("sub-1")
                .email("me@example.com")
                .nickname("지윤")
                .socialName("JH Jang")
                .name(name)
                .status("ACTIVE")
                .difficultyId(1L)
                .build();
    }

    @Test
    @DisplayName("me 는 실명까지 담아 돌려준다 — 간편인증 입력창 prefill 에 쓰인다")
    void meIncludesName() {
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        UserMeDto result = service.me(USER_ID);

        assertEquals("장재한", result.getName());
        assertEquals("지윤", result.getNickname());
        assertEquals("GOOGLE", result.getSocialProvider());
    }

    @Test
    @DisplayName("아직 인증하지 않은 사용자의 실명은 null 이다")
    void meWithoutName() {
        when(userMapper.findById(USER_ID)).thenReturn(user(null));

        assertNull(service.me(USER_ID).getName());
    }

    @Test
    @DisplayName("없는 사용자를 조회하면 NOT_FOUND")
    void meMissing() {
        when(userMapper.findById(USER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.me(USER_ID));
        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("실명을 저장하고 갱신된 사용자 정보를 돌려준다")
    void updateName() {
        when(userMapper.updateName(USER_ID, "장재한")).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        UserMeDto result = service.updateName(USER_ID, "장재한");

        assertEquals("장재한", result.getName());
        verify(userMapper).updateName(USER_ID, "장재한");
    }

    @Test
    @DisplayName("앞뒤 공백은 잘라서 저장한다")
    void updateNameTrims() {
        when(userMapper.updateName(USER_ID, "장재한")).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateName(USER_ID, "  장재한  ");

        verify(userMapper).updateName(USER_ID, "장재한");
    }

    @Test
    @DisplayName("영문 이름과 중간 공백을 허용한다")
    void updateNameAllowsEnglish() {
        when(userMapper.updateName(eq(USER_ID), anyString())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("Jaehan Jang"));

        service.updateName(USER_ID, "Jaehan Jang");

        verify(userMapper).updateName(USER_ID, "Jaehan Jang");
    }

    @Test
    @DisplayName("빈 이름·null 은 INVALID_NAME 이고 DB 를 건드리지 않는다")
    void updateNameRejectsBlank() {
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "   ")).getCode());
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, null)).getCode());

        verify(userMapper, never()).updateName(anyLong(), anyString());
    }

    @Test
    @DisplayName("한 글자·50자 초과는 INVALID_NAME")
    void updateNameRejectsLength() {
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "장")).getCode());
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "가".repeat(51))).getCode());

        verify(userMapper, never()).updateName(anyLong(), anyString());
    }

    @Test
    @DisplayName("숫자·특수문자·조합되지 않은 자모는 INVALID_NAME")
    void updateNameRejectsIllegalCharacters() {
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "장재한1")).getCode());
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "장재한!")).getCode());
        assertEquals("INVALID_NAME",
                assertThrows(BusinessException.class,
                        () -> service.updateName(USER_ID, "ㅈㅐㅎㅏㄴ")).getCode());

        verify(userMapper, never()).updateName(anyLong(), anyString());
    }

    /* ── 닉네임 (이슈 #110) ─────────────────────────────── */

    @Test
    @DisplayName("닉네임을 저장한다 — 중복 검사는 하지 않는다")
    void updateNickname() {
        when(userMapper.updateNickname(USER_ID, "탕탕이")).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateNickname(USER_ID, "탕탕이");

        verify(userMapper).updateNickname(USER_ID, "탕탕이");
    }

    @Test
    @DisplayName("닉네임 앞뒤 공백은 잘라서 저장한다")
    void updateNicknameTrims() {
        when(userMapper.updateNickname(USER_ID, "탕탕이")).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateNickname(USER_ID, "  탕탕이  ");

        verify(userMapper).updateNickname(USER_ID, "탕탕이");
    }

    @Test
    @DisplayName("닉네임은 한 글자도 허용한다 — 실명과 달리 하한이 없다")
    void updateNicknameAllowsSingleChar() {
        when(userMapper.updateNickname(eq(USER_ID), anyString())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateNickname(USER_ID, "탕");

        verify(userMapper).updateNickname(USER_ID, "탕");
    }

    @Test
    @DisplayName("닉네임은 숫자·기호도 허용한다 — 표시명이라 막을 이유가 없다")
    void updateNicknameAllowsSymbols() {
        when(userMapper.updateNickname(eq(USER_ID), anyString())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateNickname(USER_ID, "탕탕이_99!");

        verify(userMapper).updateNickname(USER_ID, "탕탕이_99!");
    }

    @Test
    @DisplayName("빈 값·공백만·50자 초과는 INVALID_REQUEST 이고 DB 를 건드리지 않는다")
    void updateNicknameRejectsInvalid() {
        for (String bad : new String[] {null, "", "   ", "가".repeat(51)}) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateNickname(USER_ID, bad));
            assertEquals("INVALID_REQUEST", ex.getCode(), "입력: " + bad);
        }
        verify(userMapper, never()).updateNickname(anyLong(), anyString());
    }

    @Test
    @DisplayName("50자는 통과한다 — 경계값")
    void updateNicknameAllows50() {
        when(userMapper.updateNickname(eq(USER_ID), anyString())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.updateNickname(USER_ID, "가".repeat(50));

        verify(userMapper).updateNickname(USER_ID, "가".repeat(50));
    }

    @Test
    @DisplayName("없는 사용자의 닉네임 갱신은 NOT_FOUND")
    void updateNicknameMissingUser() {
        when(userMapper.updateNickname(USER_ID, "탕탕이")).thenReturn(0);

        assertEquals("NOT_FOUND", assertThrows(BusinessException.class,
                () -> service.updateNickname(USER_ID, "탕탕이")).getCode());
        verify(userMapper, never()).findById(anyLong());
    }

    /* ── 표시명 규칙 (nickname ?? socialName) ────────────── */

    @Test
    @DisplayName("닉네임이 있으면 표시명은 닉네임이다")
    void displayNamePrefersNickname() {
        UserDto row = user("장재한");
        row.setNickname("탕탕이");
        row.setSocialName("JH Jang");
        when(userMapper.findById(USER_ID)).thenReturn(row);

        UserMeDto result = service.me(USER_ID);

        assertEquals("탕탕이", result.getDisplayName());
        assertEquals("JH Jang", result.getSocialName());
    }

    @Test
    @DisplayName("닉네임 미설정이면 표시명은 소셜 이름이다 — 온보딩 전 방어값")
    void displayNameFallsBackToSocialName() {
        UserDto row = user("장재한");
        row.setNickname(null);
        row.setSocialName("JH Jang");
        when(userMapper.findById(USER_ID)).thenReturn(row);

        UserMeDto result = service.me(USER_ID);

        assertEquals("JH Jang", result.getDisplayName());
        assertNull(result.getNickname(), "nickname 이 null 이어야 프론트 가드가 온보딩으로 보낸다");
    }

    @Test
    @DisplayName("닉네임이 공백뿐이어도 소셜 이름으로 넘어간다")
    void displayNameIgnoresBlankNickname() {
        UserDto row = user("장재한");
        row.setNickname("   ");
        row.setSocialName("JH Jang");
        when(userMapper.findById(USER_ID)).thenReturn(row);

        assertEquals("JH Jang", service.me(USER_ID).getDisplayName());
    }

    @Test
    @DisplayName("실명은 표시명 후보가 아니다 — 둘 다 없으면 표시명도 없다")
    void displayNameNeverUsesRealName() {
        UserDto row = user("장재한");
        row.setNickname(null);
        row.setSocialName(null);
        when(userMapper.findById(USER_ID)).thenReturn(row);

        UserMeDto result = service.me(USER_ID);

        assertNull(result.getDisplayName(), "실명(name)이 표시명으로 새어나오면 안 된다");
        assertEquals("장재한", result.getName());
    }

    /* ── 튜토리얼 완료 플래그 (이슈 #128) ───────────────── */

    @Test
    @DisplayName("메인 튜토리얼 완료는 MAIN 컬럼에 현재 시각을 남긴다")
    void markMainTutorial() {
        LocalDateTime before = LocalDateTime.now();
        when(userMapper.updateTutorialSeenAt(eq(USER_ID), eq("MAIN"), any())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.markTutorialSeen(USER_ID, TutorialType.MAIN);

        ArgumentCaptor<LocalDateTime> seenAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userMapper).updateTutorialSeenAt(eq(USER_ID), eq("MAIN"), seenAt.capture());
        assertNotNull(seenAt.getValue(), "완료 시각은 서버가 찍는다");
        assertFalse(seenAt.getValue().isBefore(before), "과거 시각이 들어가면 안 된다");
    }

    @Test
    @DisplayName("그룹 튜토리얼은 GROUP 으로 넘어간다 — 개인 것과 섞이면 안 된다")
    void markGroupTutorial() {
        when(userMapper.updateTutorialSeenAt(eq(USER_ID), eq("GROUP"), any())).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.markTutorialSeen(USER_ID, TutorialType.GROUP);

        verify(userMapper).updateTutorialSeenAt(eq(USER_ID), eq("GROUP"), any());
    }

    @Test
    @DisplayName("다시 보기는 완료 시각을 null 로 지운다")
    void clearTutorial() {
        when(userMapper.updateTutorialSeenAt(USER_ID, "MAIN", null)).thenReturn(1);
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));

        service.clearTutorialSeen(USER_ID, TutorialType.MAIN);

        verify(userMapper).updateTutorialSeenAt(USER_ID, "MAIN", null);
    }

    @Test
    @DisplayName("응답에 두 튜토리얼 시각이 그대로 실린다 — 프론트가 이 값만 보고 노출을 정한다")
    void meIncludesTutorialFlags() {
        LocalDateTime seen = LocalDateTime.of(2026, 8, 11, 10, 0);
        UserDto row = user("장재한");
        row.setTutorialSeenAt(seen);
        row.setGroupTutorialSeenAt(null);
        when(userMapper.findById(USER_ID)).thenReturn(row);

        UserMeDto result = service.me(USER_ID);

        assertEquals(seen, result.getTutorialSeenAt());
        assertNull(result.getGroupTutorialSeenAt(), "안 본 튜토리얼은 null 이어야 한다");
    }

    @Test
    @DisplayName("튜토리얼 갱신 대상이 없으면 NOT_FOUND")
    void markTutorialMissingUser() {
        when(userMapper.updateTutorialSeenAt(eq(USER_ID), eq("MAIN"), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markTutorialSeen(USER_ID, TutorialType.MAIN));

        assertEquals("NOT_FOUND", ex.getCode());
        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    @DisplayName("갱신된 행이 없으면 NOT_FOUND — 탈퇴·차단 사용자도 여기로 떨어진다")
    void updateNameMissingUser() {
        when(userMapper.updateName(USER_ID, "장재한")).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateName(USER_ID, "장재한"));

        assertEquals("NOT_FOUND", ex.getCode());
        verify(userMapper, never()).findById(anyLong());
    }

    /* ── 프로필 이미지 (이슈 #150) ───────────────────────── */

    @Test
    @DisplayName("업로드하면 새 키로 저장하고 옛 이미지를 지운다 — 순서를 뒤집으면 실패 시 원본까지 잃는다")
    void uploadReplacesOldImage() {
        UserDto before = user("장재한");
        before.setProfileImageKey("profile/7/old.jpg");
        when(userMapper.findById(USER_ID)).thenReturn(before);
        when(imageProcessor.toSquareJpeg(any())).thenReturn(new byte[]{9});
        when(imageStorage.store(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(userMapper.updateProfileImageKey(eq(USER_ID), anyString())).thenReturn(1);

        service.updateProfileImage(USER_ID, new byte[]{1, 2});

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).store(any(), key.capture());
        assertTrue(key.getValue().startsWith("profile/" + USER_ID + "/"));
        assertTrue(key.getValue().endsWith(".jpg"));
        verify(imageStorage).delete("profile/7/old.jpg");
    }

    @Test
    @DisplayName("옛 이미지가 없으면 삭제를 부르지 않는다")
    void uploadWithoutOldImage() {
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));
        when(imageProcessor.toSquareJpeg(any())).thenReturn(new byte[]{9});
        when(imageStorage.store(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(userMapper.updateProfileImageKey(eq(USER_ID), anyString())).thenReturn(1);

        service.updateProfileImage(USER_ID, new byte[]{1, 2});

        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("삭제하면 파일을 지우고 키를 비운다")
    void deleteClearsKey() {
        UserDto before = user("장재한");
        before.setProfileImageKey("profile/7/old.jpg");
        when(userMapper.findById(USER_ID)).thenReturn(before);
        when(userMapper.updateProfileImageKey(USER_ID, null)).thenReturn(1);

        service.deleteProfileImage(USER_ID);

        verify(imageStorage).delete("profile/7/old.jpg");
        verify(userMapper).updateProfileImageKey(USER_ID, null);
    }

    @Test
    @DisplayName("이미 미설정이어도 삭제는 성공이다 — 없는 것을 지우는 건 오류가 아니다")
    void deleteIsIdempotent() {
        when(userMapper.findById(USER_ID)).thenReturn(user("장재한"));
        when(userMapper.updateProfileImageKey(USER_ID, null)).thenReturn(1);

        service.deleteProfileImage(USER_ID);

        verify(imageStorage, never()).delete(anyString());
    }
}

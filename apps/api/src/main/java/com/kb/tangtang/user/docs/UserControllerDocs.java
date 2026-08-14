package com.kb.tangtang.user.docs;

import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.UserDifficultyUpdateRequestDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.dto.UserNameUpdateRequestDto;
import com.kb.tangtang.user.dto.UserNicknameUpdateRequestDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** {@code UserController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.USER)
public interface UserControllerDocs {

    @ApiOperation(
            value = "내 정보 조회",
            notes = "온보딩 진행 상태를 판별하는 기준이기도 하다. "
                    + "`nickname` 이 null 이면 아직 온보딩을 마치지 않은 사용자다. "
                    + "이것을 위한 별도 API 를 두지 않는다.")
    ApiResponse<UserMeDto> me(@ApiIgnore Long userId);

    @ApiOperation(
            value = "실명 저장",
            notes = "간편인증 화면이 인증 요청 **직전에** 호출한다.\n\n"
                    + "같은 화면에서 입력받는 생년월일·통신사·휴대폰번호는 여기로 오지 않는다. "
                    + "저장하지 않는 값이기 때문이다. 이름만 바꾸는 부분 갱신이라 PATCH 다.")
    ApiResponse<UserMeDto> updateName(@ApiIgnore Long userId,
                                      @ApiParam(value = "실명", required = true) UserNameUpdateRequestDto request);

    @ApiOperation(
            value = "닉네임(표시명) 설정·수정",
            notes = "온보딩과 마이페이지가 **같은 엔드포인트**를 쓴다. "
                    + "하는 일이 「닉네임을 이 값으로 바꾼다」 하나라 나눌 이유가 없다.")
    ApiResponse<UserMeDto> updateNickname(@ApiIgnore Long userId,
                                          @ApiParam(value = "새 닉네임", required = true) UserNicknameUpdateRequestDto request);

    @ApiOperation(
            value = "개인 미션 난이도 변경",
            notes = "**이미 배정된 오늘 미션은 바뀌지 않는다.** 다음 배정부터 적용된다.")
    ApiResponse<UserMeDto> updateDifficulty(@ApiIgnore Long userId,
                                            @ApiParam(value = "난이도 이름", required = true) UserDifficultyUpdateRequestDto request);

    @ApiOperation(
            value = "프로필 이미지 업로드",
            notes = "`multipart/form-data` 이고 **파트 이름은 `file` 고정**이다. 정사각형 JPEG 로 변환해 저장한다.\n\n"
                    + "상한(5MB)을 넘으면 본문을 읽기 전에 막는다.")
    ApiResponse<UserMeDto> uploadProfileImage(@ApiIgnore Long userId,
                                              @ApiParam(value = "이미지 파일 (최대 5MB)", required = true) MultipartFile file)
            throws IOException;

    @ApiOperation(
            value = "프로필 이미지 삭제",
            notes = "기본(이니셜) 아바타로 되돌린다. **이미 없어도 성공**이다.")
    ApiResponse<UserMeDto> deleteProfileImage(@ApiIgnore Long userId);

    @ApiOperation(
            value = "회원 탈퇴",
            notes = "소프트 딜리트 + 식별정보 익명화다. 같은 소셜 계정으로 재가입할 수 있다.\n\n"
                    + "응답 본문이 없다. 탈퇴 후에는 돌려줄 사용자 정보가 없다. "
                    + "리프레시 쿠키는 httpOnly 라 프론트가 지울 수 없어 **여기서 지운다.**\n\n"
                    + "이미 탈퇴한 계정의 재요청도 200 이다.")
    ApiResponse<Void> withdraw(@ApiIgnore Long userId, HttpServletResponse response);
}

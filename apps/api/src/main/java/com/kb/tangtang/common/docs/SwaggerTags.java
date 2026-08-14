package com.kb.tangtang.common.docs;

/**
 * Swagger 태그 이름 모음. <b>태그 하나가 문서의 섹션 하나다.</b>
 *
 * <p><b>태그는 모듈 하나에 하나다.</b> 한 모듈에 컨트롤러가 여럿이어도 같은 태그를 쓴다.
 * 컨트롤러마다 태그를 새로 만들면 섹션이 수십 개로 늘어나 문서를 훑어볼 수 없게 된다.
 * (2026-08-14 팀 결정. Swagger UI 는 태그를 평면 목록으로만 그리며 태그 안의 태그를 지원하지 않는다)
 *
 * <p>새 API 를 추가할 때는 아래 상수 중 하나를 골라 {@code @Api(tags = SwaggerTags.XXX)} 로 쓴다.
 * 문자열을 직접 적지 않는다. 오타 하나가 섹션 하나를 새로 만들어 버린다.
 *
 * <p>⚠ <b>메서드 단위로 태그를 바꿀 수는 없다.</b> {@code @ApiOperation(tags = ...)} 는
 * 클래스 태그를 덮어쓰지 않고 <b>더한다</b> — 그 엔드포인트가 두 섹션에 중복으로 나온다
 * (springfox 2.9.2 실측). 경로가 다른 도메인처럼 보여도 <b>모듈 태그를 그대로 쓴다.</b>
 * {@code TutorialControllerDocs} 가 그 예다.
 *
 * <p>정말 새 도메인이라 상수를 추가했다면 <b>{@code SwaggerConfig} 의 태그 설명 목록에도 넣는다.</b>
 * 빠뜨리면 섹션 제목만 나오고 설명이 비어 보인다.
 */
public final class SwaggerTags {

    /** 서버 상태 확인. */
    public static final String HEALTH = "00. 헬스체크";

    /** user 모듈 전체. 로그인·토큰·내 정보·동의·탈퇴, 그리고 튜토리얼 완료 처리. */
    public static final String USER = "01. 회원 · 인증";

    /** CODEF 기관 인증과 계좌 연결. */
    public static final String ACCOUNT = "02. 계좌 연동";

    /** 개인 미션. 화면 이름은 「메인 챌린지」다. */
    public static final String MISSION = "03. 개인 미션(메인 챌린지)";

    /** 그룹 챌린지. 화면 이름은 「지방법원」이다. */
    public static final String GROUP_CHALLENGE = "04. 그룹 챌린지(지방법원)";

    /** 알림 목록·읽음 처리·SSE. */
    public static final String NOTIFICATION = "05. 알림";

    /** 월간 리포트. 화면 이름은 「판결문」이다. */
    public static final String REPORT = "06. 월간 리포트(판결문)";

    /** 고정지출 후보 확정·제외와 절약 리포트. */
    public static final String FIXED_EXPENSE = "07. 고정지출 · 절약";

    /** 개발·시연 전용(/api/dev/**). SwaggerConfig 가 별도 그룹으로 분리한다. */
    public static final String DEV = "99. [DEV] 개발 · 시연 전용";

    private SwaggerTags() {
    }
}

package com.kb.tangtang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** GET /api/consents/me 응답의 항목 1건. 마이페이지 동의관리 화면이 쓴다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyConsentDto {

    private String type;
    /**
     * 이 항목이 속한 동의 그룹(SIGNUP · FINANCIAL).
     * 재동의는 POST /api/consents 로 scope 단위 저장을 하므로 화면이 이 값을 알아야 한다.
     * 프론트가 매핑을 들고 있으면 ConsentScope 가 바뀔 때 서버 정의와 어긋난다.
     */
    private String scope;
    private boolean required;
    private String label;
    private String termsUrl;
    private boolean agreed;
    /** false 면 철회 버튼을 노출하지 않는다 (TERMS·PRIVACY). */
    private boolean withdrawable;
    private String termsVersion;
    private LocalDateTime expiresAt;
}

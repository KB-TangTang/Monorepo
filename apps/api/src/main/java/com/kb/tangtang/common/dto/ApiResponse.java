package com.kb.tangtang.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 REST 응답의 공통 래퍼.
 * 컨트롤러는 반드시 이 타입으로 감싸서 반환한다. (raw 객체 반환 금지)
 *
 * 성공: { "success": true,  "data": { ... } }
 * 실패: { "success": false, "code": "NOT_FOUND", "message": "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String code;
    private final String message;

    private ApiResponse(boolean success, T data, String code, String message) {
        this.success = success;
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}

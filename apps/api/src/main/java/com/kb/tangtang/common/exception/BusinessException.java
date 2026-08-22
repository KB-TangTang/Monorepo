package com.kb.tangtang.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반을 표현하는 예외.
 * 서비스 계층에서 던지면 CommonExceptionAdvice 가 400 + code/message 로 변환한다.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

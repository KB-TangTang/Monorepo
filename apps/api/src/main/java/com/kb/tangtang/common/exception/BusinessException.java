package com.kb.tangtang.common.exception;

/**
 * 업무 규칙 위반을 표현하는 예외.
 * 서비스 계층에서 던지면 CommonExceptionAdvice 가 400 + code/message 로 변환한다.
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

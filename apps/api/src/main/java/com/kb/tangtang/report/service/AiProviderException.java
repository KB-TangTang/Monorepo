package com.kb.tangtang.report.service;

import org.springframework.http.HttpStatus;

class AiProviderException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    AiProviderException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    String getCode() {
        return code;
    }

    HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

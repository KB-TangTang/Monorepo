package com.kb.tangtang.common.exception;

import com.kb.tangtang.common.dto.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

/**
 * 전역 예외 처리. SPA + REST 구조이므로 모든 응답은 JSON 이다.
 */
@RestControllerAdvice
@Log4j2
public class CommonExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("BusinessException [{}] {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /*
     * WebConfig.MAX_FILE_SIZE(10MB) 를 넘긴 요청은 서블릿 멀티파트 파서가 던지는
     * MaxUploadSizeExceededException(= MultipartException 의 서브타입) 으로 컨트롤러에
     * 닿기도 전에 실패한다. 이건 사용자 입력 오류이지 서버 오류가 아니므로, 아래에서 잡지
     * 않으면 handleAll 이 500 INTERNAL_ERROR 로 응답한다 — 5~10MB 는 400 IMAGE_TOO_LARGE 인데
     * 10MB 초과만 500 이 되는 비일관을 막는다. 반드시 MultipartException 보다 먼저 선언할
     * 필요는 없다(Spring 은 더 구체적인 예외 타입을 우선 매칭한다).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceededException {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("IMAGE_TOO_LARGE", "5MB 이하 이미지만 올릴 수 있어요."));
    }

    /** 그 밖의 멀티파트 파싱 실패(예: file 파트 누락) — 마찬가지로 사용자가 고칠 수 있는 문제다. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipart(MultipartException ex) {
        log.warn("MultipartException {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("IMAGE_REQUIRED", "올릴 이미지를 선택해주세요."));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle404(NoHandlerFoundException ex,
                                                       HttpServletRequest request) {
        log.warn("404 - {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}

package com.allochub.global.exception;

import com.allochub.global.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(false, ex.getErrorCode().name(), ex.getMessage(), traceId(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(409)
                .body(new ApiErrorResponse(
                        false, ErrorCode.DUPLICATE.name(), "이미 등록된 출자자입니다", traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError()
                .body(new ApiErrorResponse(
                        false,
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        "서버 오류가 발생했습니다",
                        traceId(request)));
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        if (traceId instanceof String value) {
            return value;
        }
        return request.getRequestId();
    }
}

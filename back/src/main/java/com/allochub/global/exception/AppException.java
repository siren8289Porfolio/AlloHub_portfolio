package com.allochub.global.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AppException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AppException invalidInput(String message) {
        return new AppException(ErrorCode.INVALID_INPUT, message, HttpStatus.BAD_REQUEST);
    }

    public static AppException unauthorized() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    public static AppException forbidden() {
        return new AppException(ErrorCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
    }

    public static AppException duplicate(String message) {
        return new AppException(ErrorCode.DUPLICATE, message, HttpStatus.CONFLICT);
    }

    public static AppException invalidAllocationRatio(double totalRatio) {
        return new AppException(
                ErrorCode.INVALID_ALLOCATION_RATIO,
                String.format("배분 비율이 100%%를 초과합니다 (현재: %.1f%%)", totalRatio),
                HttpStatus.BAD_REQUEST);
    }

    public static AppException invalidInvestmentAmount() {
        return new AppException(
                ErrorCode.INVALID_INVESTMENT_AMOUNT,
                "투자금액이 총 출자금을 초과합니다",
                HttpStatus.BAD_REQUEST);
    }

    public static AppException distributionReconciliationFailed() {
        return new AppException(
                ErrorCode.DISTRIBUTION_RECONCILIATION_FAILED,
                "배분액 합계가 배분금액과 일치하지 않습니다",
                HttpStatus.BAD_REQUEST);
    }
}

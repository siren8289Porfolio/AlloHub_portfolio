package com.allochub.global.response;

public record ApiErrorResponse(boolean success, String errorCode, String message) {}

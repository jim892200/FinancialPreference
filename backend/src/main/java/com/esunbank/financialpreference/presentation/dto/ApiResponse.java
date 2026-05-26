package com.esunbank.financialpreference.presentation.dto;

import com.esunbank.financialpreference.common.exception.ErrorCode;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String detail) {
        return new ApiResponse<>(errorCode.code(), errorCode.message() + ": " + detail, null);
    }
}

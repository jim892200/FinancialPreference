package com.esunbank.financialpreference.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    SUCCESS              ("0000", HttpStatus.OK,                    "success"),

    VALIDATION_FAILED    ("4000", HttpStatus.BAD_REQUEST,           "validation failed"),
    INVALID_QUANTITY     ("4001", HttpStatus.BAD_REQUEST,           "invalid quantity"),
    ACCOUNT_MISMATCH     ("4002", HttpStatus.BAD_REQUEST,           "account does not match user"),

    UNAUTHORIZED         ("4010", HttpStatus.UNAUTHORIZED,          "unauthorized"),
    INVALID_CREDENTIALS  ("4011", HttpStatus.UNAUTHORIZED,          "invalid credentials"),

    FORBIDDEN            ("4030", HttpStatus.FORBIDDEN,             "forbidden"),

    USER_NOT_FOUND       ("4040", HttpStatus.NOT_FOUND,             "user not found"),
    LIKE_NOT_FOUND       ("4041", HttpStatus.NOT_FOUND,             "like item not found"),

    INTERNAL_ERROR       ("5000", HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code()        { return code; }
    public HttpStatus status()  { return status; }
    public String message()     { return message; }
}

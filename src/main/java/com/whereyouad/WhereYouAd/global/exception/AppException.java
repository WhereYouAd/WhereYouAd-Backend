package com.whereyouad.WhereYouAd.global.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final Map<String, String> bind;

    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.bind = null;
    }

    public AppException(BaseErrorCode errorCode, Map<String, String> bind) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.bind = bind;
    }
}

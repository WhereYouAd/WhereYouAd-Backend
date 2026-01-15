package com.whereyouad.WhereYouAd.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCustomCode();
    String getMessage();
}

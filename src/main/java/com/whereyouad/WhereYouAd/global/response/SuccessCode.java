package com.whereyouad.WhereYouAd.global.response;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseErrorCode {

    OK(HttpStatus.OK, "COMMON_200", "성공했습니다"),
    CREATED(HttpStatus.CREATED, "COMMON_201", "생성되었습니다");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}

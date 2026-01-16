package com.whereyouad.WhereYouAd.domains.user.exception;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_400_2", "이미 사용중인 이메일 입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}

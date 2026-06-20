package com.whereyouad.WhereYouAd.domains.notification.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    ,
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

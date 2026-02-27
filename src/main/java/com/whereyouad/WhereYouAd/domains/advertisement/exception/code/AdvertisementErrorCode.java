package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdvertisementErrorCode implements BaseErrorCode {

    // TODO: 에러 코드 추가
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

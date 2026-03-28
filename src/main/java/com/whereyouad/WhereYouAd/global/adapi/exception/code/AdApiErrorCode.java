package com.whereyouad.WhereYouAd.global.adapi.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdApiErrorCode implements BaseErrorCode {

    // 400
    INVALID_PROVIDER_VALUE(HttpStatus.BAD_REQUEST, "ADAPI_400_2", "지원하지 않는 Provider 타입입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

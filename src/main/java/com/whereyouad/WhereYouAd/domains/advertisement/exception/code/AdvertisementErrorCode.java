package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdvertisementErrorCode implements BaseErrorCode {

    // 400
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "AD_400_1", "날짜 입력이 잘못되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

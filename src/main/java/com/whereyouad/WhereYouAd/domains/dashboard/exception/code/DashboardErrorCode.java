package com.whereyouad.WhereYouAd.domains.dashboard.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashboardErrorCode implements BaseErrorCode {

    //400
    PROVIDER_NOT_VALID(HttpStatus.BAD_REQUEST, "DASH_400_1", "providerType 에 올바르지 않은 값이 입력되었습니다."),
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.whereyouad.WhereYouAd.domains.dashboard.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashBoardErrorCode implements BaseErrorCode {

    // 400
    PROVIDER_NOT_VALID(HttpStatus.BAD_REQUEST, "DASH_400_1", "providerType 에 올바르지 않은 값이 입력되었습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "DASH_400_2", "날짜 입력 방식이 잘못되었습니다."),

    // 403
    ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "DASH_403_1", "해당 조직에 대한 접근 권한이 없습니다."),
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
package com.whereyouad.WhereYouAd.domains.dashboard.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashBoardErrorCode implements BaseErrorCode {

    // 400
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "DASHBOARD_400_1", "날짜 입력이 잘못되었습니다."),

    // 403
    ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "DASHBOARD_403_1", "해당 조직에 대한 접근 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}

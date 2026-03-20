package com.whereyouad.WhereYouAd.domains.click.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ClickErrorCode implements BaseErrorCode {

    // 403
    CLICK_UNAUTHORIZED(HttpStatus.FORBIDDEN, "CLICK_403_1", "해당 광고에 접근할 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

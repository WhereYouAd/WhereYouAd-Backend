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

    // 400
    CLICK_INVALID_MODE(HttpStatus.BAD_REQUEST, "CLICK_400_1", "mode는 'real' 또는 'dummy'만 허용됩니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

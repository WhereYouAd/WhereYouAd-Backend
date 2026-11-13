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
    DUMMY_PRODUCER_DISABLED(HttpStatus.FORBIDDEN, "CLICK_403_2", "더미 클릭 생성기가 비활성화된 환경입니다."),

    // 400
    CLICK_INVALID_MODE(HttpStatus.BAD_REQUEST, "CLICK_400_1", "mode는 'real' 또는 'dummy'만 허용됩니다."),

    // 404
    TRACKING_URL_NOT_FOUND(HttpStatus.NOT_FOUND, "CLICK_404_1", "해당 광고에 발급된 트래킹 URL이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

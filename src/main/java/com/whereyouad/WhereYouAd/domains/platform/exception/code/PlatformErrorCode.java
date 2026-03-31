package com.whereyouad.WhereYouAd.domains.platform.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlatformErrorCode implements BaseErrorCode {

    // 404
    PLATFORM_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLATFORM_404_1", "조직과 연결된 인증 정보를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

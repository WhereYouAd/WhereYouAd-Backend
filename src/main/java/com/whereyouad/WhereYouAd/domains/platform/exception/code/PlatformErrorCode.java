package com.whereyouad.WhereYouAd.domains.platform.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlatformErrorCode implements BaseErrorCode {

    // 400
    NAVER_API_AUTH_FAILED(HttpStatus.BAD_REQUEST, "PLATFORM_400_1", "네이버 광고 API키 등록에 실패했습니다."),

    // 403
    PLATFORM_FORBIDDEN(HttpStatus.FORBIDDEN, "PLATFORM_403_1", "API키 등록은 ADMIN 권한이 필요합니다."),

    // 404
    PLATFORM_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLATFORM_404_1", "조직과 연결된 인증 정보를 찾을 수 없습니다."),
    PLATFORM_ORG_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLATFORM_404_2", "해당 조직의 멤버가 아닙니다."),
    PLATFORM_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "PLATFORM_404_3", "해당 광고 계정을 찾을 수 없습니다."),

    // 409
    PLATFORM_ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PLATFORM_409_1", "이미 등록된 광고 계정입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

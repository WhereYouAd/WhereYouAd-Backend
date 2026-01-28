package com.whereyouad.WhereYouAd.domains.user.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_400_2", "이미 사용중인 이메일 입니다."),
    NOT_PROVIDE_SOCIAL(HttpStatus.BAD_REQUEST, "USER400_3", "지원하지 않는 소셜 로그인 방식입니다."),

    // 401
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "USER_401_1", "인증되지 않은 소셜 이메일입니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "이메일에 해당하는 사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

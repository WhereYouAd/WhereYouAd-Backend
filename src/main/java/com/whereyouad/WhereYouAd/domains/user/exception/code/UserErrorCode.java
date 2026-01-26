package com.whereyouad.WhereYouAd.domains.user.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_400_2", "이미 사용중인 이메일 입니다."),
    USER_EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "USER_401_1", "이메일 인증이 진행되지 않았습니다."),
    USER_EMAIL_NOT_VALID(HttpStatus.BAD_REQUEST, "USER_400_3", "해당 이메일로 메일 전송에 실패했습니다."),
    USER_EMAIL_AUTH_INVALID(HttpStatus.BAD_REQUEST, "USER_400_4", "인증 코드가 올바르지 않습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

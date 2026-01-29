package com.whereyouad.WhereYouAd.domains.user.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_400_1", "이미 사용중인 이메일 입니다."),
    USER_EMAIL_NOT_VALID(HttpStatus.BAD_REQUEST, "USER_400_2", "해당 이메일로 메일 전송에 실패했습니다."),
    USER_EMAIL_AUTH_INVALID(HttpStatus.BAD_REQUEST, "USER_400_3", "인증 코드가 올바르지 않습니다."),
    USER_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "USER_400_4", "기존 비밀번호와 동일한 비밀번호로는 변경 불가합니다."),
    EMAIL_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "해당 이메일로 가입한 회원이 존재하지 않습니다."),

    USER_EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "USER_401_1", "이메일 인증이 진행되지 않았습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

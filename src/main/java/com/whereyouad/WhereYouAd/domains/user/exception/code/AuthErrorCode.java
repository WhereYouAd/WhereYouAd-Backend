package com.whereyouad.WhereYouAd.domains.user.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    //토큰 관련
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "토큰이 만료되었습니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "잘못된 토큰 형식입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "쿠키에 refreshToken 값이 존재하지 않습니다."),

    // 로그인 실패 (비밀번호 틀림 or 계정 없음)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "아이디 또는 비밀번호가 일치하지 않습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_401_4", "해당 이메일의 회원이 존재하지 않습니다."),

    // 토큰 해시 처리 실패 (SHA-256 알고리즘 미지원 등 JVM/환경 문제 -> 사실상 발생 확률 적음)
    TOKEN_HASH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_500_1", "토큰 해시 처리에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

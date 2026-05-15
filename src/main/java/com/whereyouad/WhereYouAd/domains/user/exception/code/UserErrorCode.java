package com.whereyouad.WhereYouAd.domains.user.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    // 400
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_400_1", "이미 사용중인 이메일 입니다."),
    USER_EMAIL_NOT_VALID(HttpStatus.BAD_REQUEST, "USER_400_2", "해당 이메일로 메일 전송에 실패했습니다."),
    USER_EMAIL_AUTH_INVALID(HttpStatus.BAD_REQUEST, "USER_400_3", "인증 코드가 올바르지 않습니다."),
    NOT_PROVIDE_SOCIAL(HttpStatus.BAD_REQUEST, "USER_400_4", "지원하지 않는 소셜 로그인 방식입니다."),
    USER_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "USER_400_5", "이전 비밀번호와 동일한 비밀번호로 바꿀 수 없습니다."),
    USER_PASSWORD_NOT_CORRECT(HttpStatus.BAD_REQUEST, "USER_400_6", "비밀번호가 일치하지 않습니다."),
    SOCIAL_USER_PASSWORD_CANNOT_MODIFY(HttpStatus.BAD_REQUEST, "USER_400_7", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),
    USER_OLD_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_8", "비밀번호 변경을 위해선 이전 비밀번호 입력이 필요합니다."),
    USER_OWNS_ORGANIZATION(HttpStatus.BAD_REQUEST, "USER_400_9", "다른 멤버가 속한 조직의 생성자는 탈퇴할 수 없습니다. 소유권을 위임한 뒤 다시 시도해 주세요."),

    // 401
    USER_EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "USER_401_1", "이메일 인증이 진행되지 않았습니다."),
    USER_SMS_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "USER_401_2", "휴대폰 인증이 진행되지 않았습니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "이메일에 해당하는 사용자를 찾을 수 없습니다."),
    USER_NOT_FOUND_BY_PHONE(HttpStatus.NOT_FOUND, "USER_404_2", "해당 전화번호를 가진 사용자를 찾을 수 없습니다."),

    // 500
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_500_1", "문자 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

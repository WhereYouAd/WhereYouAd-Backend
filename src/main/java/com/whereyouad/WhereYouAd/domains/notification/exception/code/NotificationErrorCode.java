package com.whereyouad.WhereYouAd.domains.notification.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_403_1", "해당 작업을 수행할 권한이 없습니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "해당 조직의 멤버를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.whereyouad.WhereYouAd.domains.notification.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    // 400
    NO_CHANNEL_CONFIGURED(HttpStatus.BAD_REQUEST, "NOTIFICATION_400_1", "발송할 외부 채널(슬랙/디스코드)이 설정되어 있지 않습니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_403_1", "해당 작업을 수행할 권한이 없습니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "해당 조직의 멤버를 찾을 수 없습니다."),
    ORG_NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "조직 알림 설정이 존재하지 않습니다."),

    // 500
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_500_1", "알림 발송에 실패했습니다."),
    NOTIFICATION_CHANNEL_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_500_2", "알림 채널(슬랙/디스코드) 설정에 실패했습니다. 웹훅 URI 확인 후 재시도 해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

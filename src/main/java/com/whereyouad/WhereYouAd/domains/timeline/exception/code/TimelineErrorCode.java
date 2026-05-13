package com.whereyouad.WhereYouAd.domains.timeline.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TimelineErrorCode implements BaseErrorCode {

    TIMELINE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "TIMELINE_400_1", "종료일은 시작일보다 이후여야 합니다."),
    TIMELINE_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMELINE_404_1", "타임라인을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

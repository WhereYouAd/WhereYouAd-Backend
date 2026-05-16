package com.whereyouad.WhereYouAd.domains.timeline.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TimelineErrorCode implements BaseErrorCode {

    // 400
    TIMELINE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "TIMELINE_400_1", "종료일은 시작일보다 이후여야 합니다."),
    TIMELINE_NO_COMPARISON_DATA(HttpStatus.BAD_REQUEST, "TIMELINE_400_2", "비교 기간에 해당하는 성과 데이터가 존재하지 않습니다."),

    // 403
    TIMELINE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "TIMELINE_403_1", "타임라인을 삭제할 권한이 없습니다."),
    TIMELINE_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "TIMELINE_403_2", "타임라인을 접근할 권한이 없습니다."),
    TIMELINE_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "TIMELINE_403_3", "타임라인 수정 권한이 없습니다."),

    // 404
    TIMELINE_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMELINE_404_1", "타임라인을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

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
    TIMELINE_NO_METRIC_DATA(HttpStatus.BAD_REQUEST, "TIMELINE_400_3", "해당 타임라인 기간의 광고 데이터가 없습니다."),
    TIMELINE_NO_CURRENT_DATA(HttpStatus.BAD_REQUEST, "TIMELINE_400_4", "선택한 기간에 해당하는 성과 데이터가 존재하지 않습니다."),
    TIMELINE_INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "TIMELINE_400_5", "올바르지 않은 타임라인 성과 상태입니다."),
    TIMELINE_INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST, "TIMELINE_400_6", "올바르지 않은 타임라인 정렬 기준입니다."),
    TIMELINE_INVALID_DISPLAY_ORDER(HttpStatus.BAD_REQUEST, "TIMELINE_400_7", "순서 변경 목록이 조직의 타임라인 목록과 일치하지 않습니다."),

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

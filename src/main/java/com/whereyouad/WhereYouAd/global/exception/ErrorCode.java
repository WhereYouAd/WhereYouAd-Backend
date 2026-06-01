package com.whereyouad.WhereYouAd.global.exception;


import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode{

    // 400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", "COMMON_400_1"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 잘못되었습니다.", "COMMON_400_2"),
    JSON_PARSE_FAIL(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패했습니다. Request body를 확인해주세요", "COMMON_400_3"),
    PARAMETER_MISMATCH(HttpStatus.BAD_REQUEST, "쿼리 파라미터 타입이 맞지 않습니다.", "COMMON_400_4"),
    NOT_FOUND_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Request body가 없습니다.", "COMMON_400_5"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON_404_1"),
    NOT_FOUND_URI(HttpStatus.NOT_FOUND, "존재하지 않는 URI입니다.", "COMMON_404_2"),

    // 405
    NOT_SUPPORT_HTTP_METHOD(HttpStatus.METHOD_NOT_ALLOWED, "잘못된 HTTP 메서드 입니다.", "COMMON_405_1"),

    // 415
    NOT_SUPPORT_CONTENT_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다.", "COMMON_415_1"),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON_500_1"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}

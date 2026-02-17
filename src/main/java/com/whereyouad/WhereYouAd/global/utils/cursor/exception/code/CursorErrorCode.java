package com.whereyouad.WhereYouAd.global.utils.cursor.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CursorErrorCode implements BaseErrorCode {

    // 400
    ID_NOT_POSITIVE_NUMBER(HttpStatus.BAD_REQUEST, "CURSOR_400_1", "ID값은 양수입니다."),
    EMPTY_CURSOR(HttpStatus.BAD_REQUEST, "CURSOR_400_2", "커서 값이 비어있습니다."),
    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "CURSOR_400_3", "잘못된 커서 형식입니다."),

    // 500
    ENCODE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CURSOR_500_1", "커서 인코딩 중 오류가 발생했습니다."),
    DECODE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CURSOR_500_2", "커서 디코딩 중 오류가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

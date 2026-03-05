package com.whereyouad.WhereYouAd.domains.image.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {

    // 400 Bad Request
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "IMAGE_400_1", "업로드할 파일이 없습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "IMAGE_400_2", "허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, webp만 가능)"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_400_3", "파일 용량이 제한을 초과했습니다."),

    // 500 Internal Server Error
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_500_1", "S3 서버로의 이미지 업로드에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

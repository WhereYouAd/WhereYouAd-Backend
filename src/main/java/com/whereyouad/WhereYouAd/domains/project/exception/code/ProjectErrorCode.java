package com.whereyouad.WhereYouAd.domains.project.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    // 403
    ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "AD_403_1", "해당 프로젝트에 대한 접근 권한이 없습니다."),

    // 404
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "AD_404_1", "존재하지 않는 프로젝트입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

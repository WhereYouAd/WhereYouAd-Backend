package com.whereyouad.WhereYouAd.domains.organization.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrgErrorCode implements BaseErrorCode {
    //400
    ORG_NAME_DUPLICATE(HttpStatus.BAD_REQUEST, "ORG_400_1", "사용자가 이미 속해있는 조직의 이름입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

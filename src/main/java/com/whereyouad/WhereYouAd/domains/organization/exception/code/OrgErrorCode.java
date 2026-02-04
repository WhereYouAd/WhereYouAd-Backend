package com.whereyouad.WhereYouAd.domains.organization.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrgErrorCode implements BaseErrorCode {
    //400
    ORG_NAME_DUPLICATE(HttpStatus.BAD_REQUEST, "ORG_400_1", "사용자가 이미 속해있는 조직의 이름입니다."),

    //403
    ORG_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "ORG_403_1", "조직 정보 변경은 해당 조직을 생성한 회원만 가능합니다."),

    //404
    ORG_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_1", "해당 id 의 조직이 존재하지 않습니다."),
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

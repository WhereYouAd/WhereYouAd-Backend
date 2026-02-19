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
    ORG_CANNOT_ADMIN_TO_MEMBER(HttpStatus.BAD_REQUEST, "ORG_400_2", "ADMIN은 MEMBER로 변경할 수 없습니다."),

    //403
    ORG_FORBIDDEN(HttpStatus.FORBIDDEN, "ORG_403_1", "해당 요청은 조직 생성자만 요청 가능합니다."),
    ORG_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "ORG_403_2", "해당 요청은 ADMIN 권한을 가진 멤버만 요청 가능합니다."),

    //404
    ORG_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_1", "해당 id 의 조직이 존재하지 않습니다."),
    ORG_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_2", "해당 조직에 속한 멤버가 존재하지 않습니다."),

    //409
    ORG_ALREADY_ACTIVE(HttpStatus.CONFLICT, "ORG_409_1", "해당 조직은 이미 활성화 상태 입니다.")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

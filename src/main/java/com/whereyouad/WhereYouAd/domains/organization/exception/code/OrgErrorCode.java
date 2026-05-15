package com.whereyouad.WhereYouAd.domains.organization.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrgErrorCode implements BaseErrorCode {
    // 400
    ORG_NAME_DUPLICATE(HttpStatus.BAD_REQUEST, "ORG_400_1", "사용자가 이미 속해있는 조직의 이름입니다."),
    ORG_CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "ORG_400_2", "자기 자신을 추방할 수 없습니다."),
    ORG_CANNOT_KICK_ADMIN(HttpStatus.BAD_REQUEST, "ORG_400_3", "ADMIN은 추방할 수 없습니다."),
    ORG_CANNOT_ROLE_CHANGE_SELF(HttpStatus.BAD_REQUEST, "ORG_400_4", "본인의 역할은 변경할 수 없습니다."),
    ORG_LAST_ADMIN(HttpStatus.BAD_REQUEST, "ORG_400_5", "마지막 ADMIN은 강등할 수 없습니다."),
    ORG_OWNER_SAME_AS_BEFORE(HttpStatus.BAD_REQUEST, "ORG_400_6", "자기 자신으로 조직 소유자를 변경할 수 없습니다."),

    // 403
    ORG_FORBIDDEN(HttpStatus.FORBIDDEN, "ORG_403_1", "해당 요청은 조직 생성자만 요청 가능합니다."),
    ORG_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "ORG_403_2", "해당 요청은 ADMIN 권한을 가진 멤버만 요청 가능합니다."),
    ORG_INVITATION_FORBIDDEN_USER(HttpStatus.FORBIDDEN, "ORG_INVITATION_403_1", "초대된 이메일과 현재 로그인한 사용자의 이메일이 일치하지 않습니다."),

    // 404
    ORG_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_1", "해당 id 의 조직이 존재하지 않습니다."),
    ORG_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_2", "해당 멤버가 조직에 존재하지 않습니다."),

    //409
    ORG_ALREADY_ACTIVE(HttpStatus.CONFLICT, "ORG_409_1", "해당 조직은 이미 활성화 상태 입니다."),
    ORG_MEMBER_ALREADY_ACTIVE(HttpStatus.CONFLICT, "ORG_MEMBER_409_1", "이미 해당 조직에 초대되어있습니다."),
    ORG_ALREADY_INVITE(HttpStatus.CONFLICT, "ORG_MEMBER_409_2", "초대 메시지 재전송은 5분 이후 가능합니다."),

    //410
    ORG_SOFT_DELETED(HttpStatus.GONE, "ORG_410_1", "해당 조직은 삭제된 조직입니다.(Soft Delete)"),
    ORG_INVITATION_INVALID(HttpStatus.BAD_REQUEST, "ORG_INVITATION_400", "조직 초대 토큰이 만료되었거나 유효하지 않습니다."),

    //500
    ORG_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORG_500_1", "조직 생성 중 서버 오류가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

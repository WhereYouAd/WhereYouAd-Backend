package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GoogleAdErrorCode implements BaseErrorCode {

    NOT_ACCOUNT_OWNER(HttpStatus.FORBIDDEN, "GOOGLE_403_1", "해당 Google 광고 계정을 연동한 사용자만 예산을 변경할 수 있습니다."),
    SAME_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "GOOGLE_400_1", "기존 예산과 동일한 금액으로 변경할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

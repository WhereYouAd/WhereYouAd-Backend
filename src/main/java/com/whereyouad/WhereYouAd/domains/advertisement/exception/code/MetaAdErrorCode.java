package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MetaAdErrorCode implements BaseErrorCode {

    // 400
    INVALID_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "META_400_1", "예산이 Meta 최소 기준 미만입니다. 통화·최적화 방식에 따라 최소 예산이 다를 수 있습니다."),
    INVALID_BUDGET_TYPE(HttpStatus.BAD_REQUEST, "META_400_2", "Meta 광고에서 현재 설정된 예산 유형과 다른 유형으로 변경할 수 없습니다. 기존에 설정된 예산 유형(일일 예산/총 예산)에 맞춰 요청해주세요."),
    SAME_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "META_400_3", "이전 예산과 동일한 값으로 수정할 수 없습니다."),
    BUDGET_NOT_ON_CAMPAIGN(HttpStatus.BAD_REQUEST, "META_400_4", "이 캠페인은 캠페인 레벨 예산을 사용하지 않습니다. 광고세트(광고그룹) 예산을 변경해주세요."),
    BUDGET_NOT_ON_ADGROUP(HttpStatus.BAD_REQUEST, "META_400_5", "이 광고세트는 자체 예산이 없습니다. 캠페인 예산 최적화(CBO)가 적용된 경우 캠페인 예산을 변경해주세요."),

    // 403
    NOT_ACCOUNT_OWNER(HttpStatus.FORBIDDEN, "META_403_1", "해당 Meta 광고 계정을 연동한 사용자만 예산을 변경할 수 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

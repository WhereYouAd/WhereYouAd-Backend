package com.whereyouad.WhereYouAd.domains.ai.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AIErrorCode implements BaseErrorCode {

    // 400
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "AI_400_1", "날짜 범위가 유효하지 않습니다."),
    INVALID_OPENAI_REQUEST(HttpStatus.BAD_REQUEST, "AI_400_2", "OpenAI API 요청 파라미터가 올바르지 않습니다."),

    // 401
    INVALID_OPENAI_API_KEY(HttpStatus.UNAUTHORIZED, "AI_401_2", "OpenAI API 키가 유효하지 않습니다."),

    // 403
    AI_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "AI_403_1", "해당 리포트에 대한 접근 권한이 없습니다."),

    // 404
    NO_METRIC_DATA(HttpStatus.NOT_FOUND, "AI_404_1", "해당 기간의 광고 데이터가 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_404_2", "해당 분석 리포트를 찾을 수 없습니다."),

    // 429
    OPENAI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "AI_429_1", "OpenAI API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // 500
    AI_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_500_1", "AI 분석 요청에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

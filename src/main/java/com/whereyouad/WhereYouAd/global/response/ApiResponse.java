package com.whereyouad.WhereYouAd.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"code", "message", "success", "data"})
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL) //data 값이 null 이면 반환 JSON 에서 data 필드 자체가 사라진다
    private final T data;

    // =============================
    // 성공 시 응답 생성 메서드
    // =============================

    /**
     * 성공 응답 (데이터 있음)
     * 예: 조회 성공, 생성 후 결과 반환
     */
    public static <T> ApiResponse<T> ofSuccess(BaseErrorCode code, T data) {
        return new ApiResponse<>(true, code.getCustomCode(), code.getMessage(), data);
    }

    /**
     * 성공 응답 (데이터 없음)
     * 예: 삭제 성공, 수정 성공 (데이터 반환 안 할 때)
     */
    public static <T> ApiResponse<T> ofSuccess(BaseErrorCode code) {
        return new ApiResponse<>(true, code.getCustomCode(), code.getMessage(), null);
    }

    // =============================
    // 실패 시 응답 생성 메서드 (예외 핸들러에서 사용)
    // =============================

    public static <T> ApiResponse<T> ofFailure(BaseErrorCode code) {
        return new ApiResponse<>(false, code.getCustomCode(), code.getMessage(), null);
    }
}

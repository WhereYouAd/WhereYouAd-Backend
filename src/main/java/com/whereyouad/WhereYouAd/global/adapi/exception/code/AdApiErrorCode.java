package com.whereyouad.WhereYouAd.global.adapi.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdApiErrorCode implements BaseErrorCode {

    // 400
    INVALID_PROVIDER_VALUE(HttpStatus.BAD_REQUEST, "ADAPI_400_2", "지원하지 않는 Provider 타입입니다."),
    
    // 500
    GOOGLE_TOKEN_REFRESH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_1", "구글 광고 API 토큰 갱신에 실패했습니다."),
    GOOGLE_TOKEN_ENCRYPTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_2", "구글 인증 토큰 암호화 중 오류가 발생했습니다."),
    GOOGLE_API_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_3", "구글 광고 API 호출 중 오류가 발생했습니다."),
    GOOGLE_DATA_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_4", "구글 광고 데이터(JSON 파싱 및 저장) 동기화에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

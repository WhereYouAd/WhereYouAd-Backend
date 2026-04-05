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
    INVALID_API_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ADAPI_401_1", "광고 플랫폼 연동 인증 정보(토큰 등)가 유효하지 않거나 만료되었습니다."),
    TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "ADAPI_400_3", "액세스 토큰을 발급받거나 장기 토큰으로 연장하는 데 실패했습니다. 인증이 만료되었거나 유효하지 않습니다."),
    AD_ACCOUNT_FETCH_FAILED(HttpStatus.BAD_REQUEST, "ADAPI_400_4", "광고 플랫폼에서 연동할 수 있는 광고 계정 목록을 조회하는 데 실패했습니다."),
    NO_LINKABLE_AD_ACCOUNT(HttpStatus.BAD_REQUEST, "ADAPI_400_5", "연동할 수 있는 광고 계정이 존재하지 않습니다. 플랫폼 관리자 센터에서 광고 계정을 먼저 생성해주세요."),

    // 500
    SYNC_DATA_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_1", "광고 데이터 동기화 및 파싱 처리 중 예상치 못한 에러가 발생했습니다."),
    CONNECTION_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_2", "광고 플랫폼 연동 정보(토큰 등)를 암호화하여 저장하는 중 내부 서버 에러가 발생했습니다."),
    EXTERNAL_API_COMMUNICATION_ERROR(HttpStatus.BAD_GATEWAY, "ADAPI_502_1", "외부 광고 플랫폼 API 통신 중 에러가 발생했습니다."),
    TOKEN_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "ADAPI_502_2", "광고 플랫폼으로부터 비어있는 토큰 응답을 받았습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

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
    INVALID_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "ADAPI_400_6", "예산이 Meta 최소 기준 미만입니다. 통화·최적화 방식에 따라 최소 예산이 다를 수 있습니다."),
    BUDGET_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "ADAPI_400_7", "Meta 의 광고그룹(AdSet)은 일일 예산만 변경할 수 있습니다. 총 예산은 캠페인에서 설정해주세요."),
    INVALID_BUDGET_TYPE(HttpStatus.BAD_REQUEST, "ADAPI_400_8", "현재 설정된 예산 유형과 다른 유형으로 변경할 수 없습니다. 기존에 설정된 예산 유형(일일 예산/총 예산)에 맞춰 요청해주세요."),
    SAME_BUDGET_AMOUNT(HttpStatus.BAD_REQUEST, "ADAPI_400_9", "예산 값을 이전 예산과 동일한 값으로 수정할 수 없습니다."),

    // 403
    NOT_ACCOUNT_OWNER(HttpStatus.FORBIDDEN, "ADAPI_403_2", "해당 광고 계정을 연동한 사용자만 예산을 변경할 수 있습니다."),

    // 409
    SYNC_IN_PROGRESS(HttpStatus.CONFLICT, "ADAPI_409_1", "해당 조직의 광고 데이터 동기화가 이미 진행 중입니다. 잠시 후 다시 시도해주세요."),
    // 429
    SYNC_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "ADAPI_429_1", "너무 잦은 동기화 요청입니다. 잠시 후 다시 시도해주세요."),

    // 500
    SYNC_DATA_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_1", "광고 데이터 동기화 및 파싱 처리 중 예상치 못한 에러가 발생했습니다."),
    CONNECTION_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ADAPI_500_2", "광고 플랫폼 연동 정보(토큰 등)를 암호화하여 저장하는 중 내부 서버 에러가 발생했습니다."),
    EXTERNAL_API_COMMUNICATION_ERROR(HttpStatus.BAD_GATEWAY, "ADAPI_502_1", "외부 광고 플랫폼 API 통신 중 에러가 발생했습니다."),
    TOKEN_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "ADAPI_502_2", "광고 플랫폼으로부터 비어있는 토큰 응답을 받았습니다."),
    BUDGET_UPDATE_FAILED(HttpStatus.BAD_GATEWAY, "ADAPI_502_3", "예산 변경에 실패했습니다."),

    // OAuth 연동 관련
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "ADAPI_400_1", "유효하지 않거나 만료된 구글 연동 요청입니다."),
    OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ADAPI_403_1", "사용자가 구글 연동 권한을 거부했습니다."),

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

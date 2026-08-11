package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NaverAdErrorCode implements BaseErrorCode {

    // 400
    NAVER_INVALID_DOWNLOAD_URL(HttpStatus.BAD_REQUEST, "NAVER_400_1", "유효하지 않은 다운로드 URL입니다."),
    NAVER_INVALID_BUDGET_VALUE(HttpStatus.BAD_REQUEST, "NAVER_400_2", "예산 및 입찰가는 10원 단위로 입력해야 합니다."),
    NAVER_INVALID_BUDGET_RANGE(HttpStatus.BAD_REQUEST, "NAVER_400_3", "예산은 50원 이상 1,000,000,000원 이하로 입력해야 합니다."),
    NAVER_INVALID_BID_AMOUNT_RANGE(HttpStatus.BAD_REQUEST, "NAVER_400_4", "입찰가는 70원 이상 100,000원 이하로 입력해야 합니다."),
    NAVER_SAME_BUDGET_VALUE(HttpStatus.BAD_REQUEST, "NAVER_400_5", "이전 예산과 동일한 값으로 수정할 수 없습니다."),
    NAVER_INVALID_SYNC_RANGE(HttpStatus.BAD_REQUEST, "NAVER_400_6", "동기화 기간은 최대 365일까지 가능합니다."),

    // 404
    NAVER_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "NAVER_404_1", "플랫폼 연결 정보를 찾을 수 없습니다."),

    // 500
    NAVER_CAMPAIGN_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_1", "네이버 캠페인 목록 조회에 실패했습니다."),
    NAVER_AD_GROUP_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_2", "네이버 광고 그룹 목록 조회에 실패했습니다."),
    NAVER_AD_CONTENT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_3", "네이버 광고 소재 목록 조회에 실패했습니다."),
    NAVER_KEYWORD_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_4", "네이버 키워드 목록 조회에 실패했습니다."),
    NAVER_REPORT_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_5", "네이버 성과 보고서 생성 요청에 실패했습니다."),
    NAVER_REPORT_STATUS_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_6", "네이버 성과 보고서 상태 조회에 실패했습니다."),
    NAVER_REPORT_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_7", "네이버 성과 보고서 원문 다운로드에 실패했습니다."),
    NAVER_HOURLY_STAT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_8", "네이버 시간대별 통계 조회에 실패했습니다."),
    NAVER_CAMPAIGN_BUDGET_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_9", "네이버 캠페인 예산 수정에 실패했습니다."),
    NAVER_AD_GROUP_BUDGET_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_10", "네이버 광고 그룹 예산 수정에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.whereyouad.WhereYouAd.domains.advertisement.exception.code;

import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NaverAdErrorCode implements BaseErrorCode {


    // 500
    NAVER_CAMPAIGN_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_1", "네이버 캠페인 목록 조회에 실패했습니다."),
    NAVER_AD_GROUP_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_2", "네이버 광고 그룹 목록 조회에 실패했습니다."),
    NAVER_AD_CONTENT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_3", "네이버 광고 소재 목록 조회에 실패했습니다."),
    NAVER_KEYWORD_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_4", "네이버 키워드 목록 조회에 실패했습니다."),
    NAVER_REPORT_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_5", "네이버 성과 보고서 생성 요청에 실패했습니다."),
    NAVER_REPORT_STATUS_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_6", "네이버 성과 보고서 상태 조회에 실패했습니다."),
    NAVER_REPORT_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_7", "네이버 성과 보고서 원문 다운로드에 실패했습니다."),
    NAVER_HOURLY_STAT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_8", "네이버 시간대별 통계 조회에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

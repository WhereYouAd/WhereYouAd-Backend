package com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.time.LocalDate;
import java.util.List;

public class AdvertisementResponse {

    // Provider별 ROAS 개별 순위
    public record RankingROAS(
            Integer rank, // 순위 (ROAS 내림차순)
            Provider provider, // 광고 플랫폼 (GOOGLE, KAKAO, NAVER)
            Double roas, // ROAS = (revenue / adSpend) * 100
            Integer diffRate, // 이전 동일 기간 대비 ROAS 변화율 (%), null = 비교 데이터 없음
            Long revenue, // 매출 합계 (원)
            Long adSpend // 광고비 합계 (원)
        ) {
    }

    // ROAS 순위 목록 전체 리스트
    public record RankingROASList(
            LocalDate startDate,
            LocalDate endDate,
            List<RankingROAS> rankings) {
    }
}

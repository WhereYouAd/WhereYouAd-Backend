package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;

import java.time.LocalDate;

public interface AdvertisementQueryService {

    // 특정 조직의 전체 광고에 대한 기간별 ROAS 성과 순위 조회
    AdvertisementResponse.RankingROASList getRoasRanking(Long userId, Long orgId, LocalDate startDate, LocalDate endDate);
}

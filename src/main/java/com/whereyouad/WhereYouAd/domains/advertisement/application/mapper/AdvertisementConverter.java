package com.whereyouad.WhereYouAd.domains.advertisement.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.math.BigDecimal;

public class AdvertisementConverter {

    public static AdvertisementResponse.RankingROAS toRankingROAS(
            Integer rank,
            Provider provider,
            Double roas,
            Integer diffRate,
            BigDecimal totalRevenue,
            BigDecimal totalSpend) {
        return new AdvertisementResponse.RankingROAS(
                rank,
                provider,
                Math.round(roas * 100.0) / 100.0, // 소수점 2자리 반올림
                diffRate,
                totalRevenue != null ? totalRevenue.longValue() : 0L,
                totalSpend != null ? totalSpend.longValue() : 0L);
    }
}

package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection;

import java.math.BigDecimal;

public interface RoasProjection {

    String getProvider(); // "GOOGLE", "KAKAO", "NAVER" 등 문자열로 수신

    BigDecimal getTotalRevenue(); // SUM(revenue), 총 매출액 합계

    BigDecimal getTotalSpend(); // SUM(spend), 총 광고비 합계
}
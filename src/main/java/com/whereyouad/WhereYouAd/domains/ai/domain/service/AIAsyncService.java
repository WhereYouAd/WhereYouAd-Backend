package com.whereyouad.WhereYouAd.domains.ai.domain.service;

import java.time.LocalDate;

public interface AIAsyncService {

    // 분석 요청 비동기 처리 메서드
    void analyzeAsync(Long reportId, Long orgId, String provider, LocalDate startDate, LocalDate endDate);
}

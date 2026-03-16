package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;

public interface ClickService {

    // 트래킹 링크 생성 메서드
    ClickResponse.NewTrackingUrl createTrackingUrl(Long userId, Long adContentId, Long orgId);
}

package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;

public interface ClickService {

    // 트래킹 링크 생성 메서드
    ClickResponse.NewTrackingUrl createTrackingUrl(Long userId, Long adContentId, Long orgId);

    // 트래킹 링크 접속 시 랜딩 Url 반환 및 이벤트 생성
    String handleTrackingRedirect(String code, String ipAddress, String userAgent);
}

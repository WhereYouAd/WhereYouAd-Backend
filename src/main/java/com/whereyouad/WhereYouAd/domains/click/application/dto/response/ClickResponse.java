package com.whereyouad.WhereYouAd.domains.click.application.dto.response;

import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;

import java.time.LocalDateTime;

public class ClickResponse {

    // 새로운 트래킹 링크 발급 응답
    public record NewTrackingUrl(
            String trackingUrl
    ) {}

    // 클릭 로그 응답 저장 DTO
    public record ClickEvent(
            Long adContentId,
            String ipAddress,
            String userAgent,
            LocalDateTime clickedAt
    ) {}
}

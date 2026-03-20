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

    // 실시간 클릭 수 조회 응답 DTO (분 단위)
    public record RealtimeClickCount(
            String minute,   // ex) "202603201830"
            Long count
    ) {}
}

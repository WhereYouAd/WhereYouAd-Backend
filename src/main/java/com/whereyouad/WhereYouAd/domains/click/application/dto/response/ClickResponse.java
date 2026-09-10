package com.whereyouad.WhereYouAd.domains.click.application.dto.response;

public class ClickResponse {

    // 새로운 트래킹 링크 발급 응답
    public record NewTrackingUrl(
            String trackingUrl
    ) {}

    // 트래킹 링크 삭제 응답
    public record DeletedTrackingUrl(
            String deletedTrackingUrl   // 폐기된 트래킹 URL
    ) {}

    // 실시간 클릭 수 조회 응답 DTO (분 단위)
    public record RealtimeClickCount(
            String minute,   // ex) "202603201830"
            Long count
    ) {}
}


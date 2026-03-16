package com.whereyouad.WhereYouAd.domains.click.application.dto.response;

public class ClickResponse {

    // 새로운 트래킹 링크 발급 응답
    public record NewTrackingUrl(
            String trackingUrl
    ) {}
}

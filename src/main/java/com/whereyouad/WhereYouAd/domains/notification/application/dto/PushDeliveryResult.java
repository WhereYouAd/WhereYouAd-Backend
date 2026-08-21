package com.whereyouad.WhereYouAd.domains.notification.application.dto;

// 웹 푸시 1건 발송 결과. statusCode=0 은 네트워크/서명 예외로 HTTP 응답을 못 받은 경우
public record PushDeliveryResult(
        Long deliveryId,
        Long subscriptionId,
        int statusCode,
        String failReason
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    // 브라우저 push service 가 구독 만료/폐기를 알리는 응답 코드
    public boolean isExpired() {
        return statusCode == 404 || statusCode == 410;
    }

    public String failReasonSummary() {
        if (isSuccess()) return null;
        if (failReason != null && !failReason.isBlank()) {
            return "HTTP " + statusCode + " - " + failReason;
        }
        return "HTTP " + statusCode;
    }
}

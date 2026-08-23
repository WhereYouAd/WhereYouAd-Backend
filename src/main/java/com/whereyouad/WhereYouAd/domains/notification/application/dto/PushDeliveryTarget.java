package com.whereyouad.WhereYouAd.domains.notification.application.dto;

// 웹 푸시 1건의 발송 대상. subscriptionId=null 인 경우 = 해당 멤버에 등록된 구독이 없다는 뜻(즉시 실패)
// 생성은 NotificationConverter.toPushDeliveryTarget / toEmptyPushDeliveryTarget 사용
public record PushDeliveryTarget(
        Long deliveryId,
        Long membershipId,
        Long subscriptionId,
        String endpoint,
        String p256dhKey,
        String authSecret
) {
    public boolean hasSubscription() {
        return subscriptionId != null;
    }
}

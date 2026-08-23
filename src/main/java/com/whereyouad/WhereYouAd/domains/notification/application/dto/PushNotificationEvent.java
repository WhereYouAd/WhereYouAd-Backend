package com.whereyouad.WhereYouAd.domains.notification.application.dto;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 브라우저 푸시(웹 푸시) 발송을 위해 Kafka 로 발행되는 이벤트
// 최초 발행/재시도 모두 동일 payload 사용. notificationId 는 이미 저장된 Notification 을 재사용
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushNotificationEvent {

    private Long orgId;

    // Notification 엔티티 PK. 재시도 스케줄러가 조회 후 재발행할 때 참조
    private Long notificationId;

    // 재시도 이벤트에서 선택된 NotificationDelivery PK. null/empty 면 최초 발송 이벤트
    private List<Long> deliveryIds;

    private NotificationType type;
    private String title;
    private String body;
    private String linkUrl;
}
